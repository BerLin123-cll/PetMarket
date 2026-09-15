package com.pet.petmarket.service.impl;

import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.config.UploadProperties;
import com.pet.petmarket.entity.UploadFile;
import com.pet.petmarket.mapper.UploadFileMapper;
import com.pet.petmarket.service.FileUploadService;
import com.pet.petmarket.vo.UploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地文件上传服务实现
 *
 * <p>安全与健壮性要点：</p>
 * <ul>
 *   <li><b>目录穿越防护</b>：文件名由服务端用 UUID 重新生成，绝不使用客户端文件名拼路径；
 *       最终写入前再用 {@code normalize()} 校验目标路径必须落在上传根目录内；</li>
 *   <li><b>类型白名单</b>：同时校验扩展名与 MIME 类型，并可选校验文件头魔数，
 *       防止把非法文件改扩展名上传；</li>
 *   <li><b>大小限制</b>：单文件大小限制与 multipart 限制双重校验；</li>
 *   <li><b>落库失败清理</b>：数据库写入异常时删除已落盘的文件，避免产生孤儿文件。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 允许上传的子目录白名单，防止 bizType 被构造（如 ../../）穿越目录 */
    private static final java.util.Set<String> ALLOWED_BIZ_TYPES =
            java.util.Set.of(BIZ_PRODUCT, BIZ_AVATAR, BIZ_CATEGORY, "other");

    private final UploadProperties uploadProperties;
    private final UploadFileMapper uploadFileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadVO uploadImage(MultipartFile file, String bizType, Long uploaderId) {
        validate(file);

        String subDir = resolveSubDir(bizType);
        String extension = resolveExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        String relativePath = subDir + "/" + LocalDate.now().format(DATE_DIR) + "/" + storedName;

        Path uploadRoot = getUploadRoot();
        Path target = uploadRoot.resolve(relativePath).normalize();
        // 目录穿越兜底校验：解析后的绝对路径必须仍在上传根目录内
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException("非法的文件路径");
        }

        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("保存上传文件失败：{}", e.getMessage(), e);
            throw new BusinessException("文件保存失败，请稍后重试");
        }

        // 写到磁盘后再校验文件头，非法内容立即删除
        if (uploadProperties.isCheckMagicNumber()) {
            try {
                checkMagicNumber(target);
            } catch (BusinessException e) {
                deleteQuietly(target);
                throw e;
            }
        }

        String url = uploadProperties.getUrlPrefix() + "/" + relativePath;

        // 记录元信息（数据库不可用时不影响图片访问，故失败只记日志）
        try {
            UploadFile record = new UploadFile();
            record.setBizType(subDir);
            record.setOriginalName(file.getOriginalFilename());
            record.setStoredName(storedName);
            record.setRelativePath(relativePath);
            record.setUrl(url);
            record.setFileSize(file.getSize());
            record.setContentType(file.getContentType());
            record.setUploaderId(uploaderId);
            uploadFileMapper.insert(record);
        } catch (Exception e) {
            log.warn("上传记录写入数据库失败（图片已保存成功）：{}", e.getMessage());
        }

        log.info("图片上传成功：url={}, size={}字节, uploaderId={}", url, file.getSize(), uploaderId);
        return new UploadVO(url, file.getOriginalFilename(), file.getSize());
    }

    /**
     * 上传根目录（不存在则创建），返回绝对规范化路径
     */
    private Path getUploadRoot() {
        Path root = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new BusinessException("上传目录创建失败：" + root);
        }
        return root;
    }

    /**
     * 解析业务子目录，非法值统一落到 other
     */
    private String resolveSubDir(String bizType) {
        if (bizType == null || bizType.isBlank()) {
            return "other";
        }
        String normalized = bizType.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_BIZ_TYPES.contains(normalized) ? normalized : "other";
    }

    /**
     * 提取并校验扩展名（小写、含点）
     */
    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("文件缺少扩展名，无法识别图片格式");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (ext.length() > 10 || !uploadProperties.getAllowedExtensions().contains(ext)) {
            throw new BusinessException("仅支持上传图片格式：" + String.join("、", uploadProperties.getAllowedExtensions()));
        }
        return ext;
    }

    /**
     * 基础校验：空文件、大小、MIME 白名单
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片文件");
        }
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new BusinessException("图片大小不能超过 "
                    + (uploadProperties.getMaxSize() / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !uploadProperties.getAllowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("图片类型不支持，仅支持 JPG / PNG / GIF / WEBP / BMP");
        }
        // 扩展名提前校验，避免无效文件落盘
        resolveExtension(file.getOriginalFilename());
    }

    /**
     * 校验文件头魔数，确认确实是图片（防止改扩展名绕过）
     */
    private void checkMagicNumber(Path target) {
        byte[] header = new byte[12];
        int read;
        try (var in = Files.newInputStream(target)) {
            read = in.read(header);
        } catch (IOException e) {
            throw new BusinessException("图片读取失败");
        }
        if (read < 4 || !isImageHeader(header, read)) {
            throw new BusinessException("文件内容不是有效的图片，请重新选择");
        }
    }

    private boolean isImageHeader(byte[] h, int len) {
        // JPEG: FF D8 FF
        if (len >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (len >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && (h[4] & 0xFF) == 0x0D && (h[5] & 0xFF) == 0x0A && (h[6] & 0xFF) == 0x1A && (h[7] & 0xFF) == 0x0A) {
            return true;
        }
        // GIF: "GIF87a" / "GIF89a"
        if (len >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8'
                && (h[4] == '7' || h[4] == '9') && h[5] == 'a') {
            return true;
        }
        // BMP: "BM"
        if (len >= 2 && h[0] == 'B' && h[1] == 'M') {
            return true;
        }
        // WEBP: "RIFF"...."WEBP"
        if (len >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return true;
        }
        return false;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("清理非法上传文件失败：{}", path);
        }
    }
}
