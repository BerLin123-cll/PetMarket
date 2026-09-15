package com.pet.petmarket.service.impl;

import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.config.UploadProperties;
import com.pet.petmarket.entity.UploadFile;
import com.pet.petmarket.mapper.UploadFileMapper;
import com.pet.petmarket.vo.UploadVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 本地图片上传服务单元测试
 *
 * <p>验收点：正常上传落盘并返回可访问 URL、扩展名/MIME/文件头魔数校验、大小限制、
 * 目录穿越防护、以及数据库记录写入不阻断上传结果。</p>
 */
class FileUploadServiceImplTest {

    @TempDir
    Path tempDir;

    private UploadProperties properties;
    private UploadFileMapper uploadFileMapper;
    private FileUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new UploadProperties();
        properties.setDir(tempDir.toString());
        properties.setUrlPrefix("/uploads");
        properties.setMaxSize(5 * 1024 * 1024);

        uploadFileMapper = mock(UploadFileMapper.class);
        when(uploadFileMapper.insert(any(UploadFile.class))).thenReturn(1);

        service = new FileUploadServiceImpl(properties, uploadFileMapper);
    }

    /** 一张最小合法 PNG（1x1 透明像素），用于验证文件头魔数校验 */
    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R'
        };
    }

    private byte[] jpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F'};
    }

    private MockMultipartFile image(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }

    @Test
    @DisplayName("验收1：上传合法 PNG 成功，文件落盘且返回 /uploads 访问地址")
    void uploadPngSuccess() throws IOException {
        UploadVO vo = service.uploadImage(image("宠物图.png", "image/png", pngBytes()), "product", 1L);

        assertNotNull(vo);
        assertNotNull(vo.getUrl());
        assertTrue(vo.getUrl().startsWith("/uploads/product/"), "URL 前缀与业务目录应正确：" + vo.getUrl());
        assertTrue(vo.getUrl().endsWith(".png"));
        assertEquals("宠物图.png", vo.getName());
        assertEquals((long) pngBytes().length, vo.getSize());

        // 校验文件真正写到了磁盘，且路径与返回的 URL 一致
        String relative = vo.getUrl().substring("/uploads/".length());
        Path saved = tempDir.resolve(relative);
        assertTrue(Files.exists(saved), "文件应已保存到 " + saved);
        assertArrayEquals(pngBytes(), Files.readAllBytes(saved));
    }

    @Test
    @DisplayName("验收2：文件名由服务端 UUID 重建，避免同名覆盖与路径穿越")
    void storedNameIsUuidAndPathIsSafe() {
        UploadVO first = service.uploadImage(image("../../evil.png", "image/png", pngBytes()), "product", 1L);
        UploadVO second = service.uploadImage(image("../../evil.png", "image/png", pngBytes()), "product", 1L);

        assertNotEquals(first.getUrl(), second.getUrl(), "同一文件名两次上传应生成不同地址");
        assertFalse(first.getUrl().contains(".."), "访问地址不应包含路径穿越字符：" + first.getUrl());
        assertTrue(Paths.get(tempDir.toString(), first.getUrl().substring("/uploads/".length()))
                .normalize().startsWith(tempDir.toAbsolutePath().normalize()));
    }

    @Test
    @DisplayName("验收3：非图片扩展名被拒绝")
    void rejectNonImageExtension() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadImage(image("恶意脚本.js", "image/png", pngBytes()), "product", 1L));
        assertTrue(ex.getMessage().contains("仅支持上传图片格式"), ex.getMessage());
    }

    @Test
    @DisplayName("验收4：MIME 类型不在白名单被拒绝")
    void rejectNonImageContentType() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadImage(image("fake.png", "application/x-msdownload", pngBytes()), "product", 1L));
        assertTrue(ex.getMessage().contains("图片类型不支持"), ex.getMessage());
    }

    @Test
    @DisplayName("验收5：改扩展名伪装成图片的文件被文件头魔数校验拦下并清理落盘文件")
    void rejectFakeImageByMagicNumber() {
        byte[] notImage = "这是一个伪装成 PNG 的文本文件".getBytes();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadImage(image("fake.png", "image/png", notImage), "product", 1L));
        assertTrue(ex.getMessage().contains("不是有效的图片"), ex.getMessage());

        // 落盘的非法文件应已被清理
        Path productDir = tempDir.resolve("product");
        if (Files.exists(productDir)) {
            try (var stream = Files.walk(productDir)) {
                long fileCount = stream.filter(Files::isRegularFile).count();
                assertEquals(0, fileCount, "非法文件不应留在上传目录");
            } catch (IOException e) {
                fail("遍历上传目录失败：" + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("验收6：超过大小限制被拒绝")
    void rejectOversizeFile() {
        properties.setMaxSize(10);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadImage(image("big.png", "image/png", pngBytes()), "product", 1L));
        assertTrue(ex.getMessage().contains("不能超过"), ex.getMessage());
    }

    @Test
    @DisplayName("验收7：空文件被拒绝")
    void rejectEmptyFile() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadImage(image("empty.png", "image/png", new byte[0]), "product", 1L));
        assertTrue(ex.getMessage().contains("请选择要上传的图片"), ex.getMessage());
    }

    @Test
    @DisplayName("验收8：未在白名单内的 bizType 归入 other 目录（防止目录穿越）")
    void unknownBizTypeFallsBackToOther() {
        UploadVO vo = service.uploadImage(image("a.png", "image/png", pngBytes()), "../../etc", 1L);
        assertTrue(vo.getUrl().startsWith("/uploads/other/"), vo.getUrl());
    }

    @Test
    @DisplayName("验收9：JPG 文件同样可上传，头像业务写入 avatar 目录")
    void uploadJpegAsAvatar() {
        UploadVO vo = service.uploadImage(image("avatar.jpg", "image/jpeg", jpegBytes()), "avatar", 9L);
        assertTrue(vo.getUrl().startsWith("/uploads/avatar/"), vo.getUrl());
        assertTrue(vo.getUrl().endsWith(".jpg"));
    }

    @Test
    @DisplayName("验收10：数据库记录写入失败不影响图片上传成功（降级容错）")
    void databaseFailureDoesNotBreakUpload() {
        when(uploadFileMapper.insert(any(UploadFile.class))).thenThrow(new RuntimeException("模拟数据库不可用"));

        UploadVO vo = service.uploadImage(image("ok.png", "image/png", pngBytes()), "product", 1L);

        assertNotNull(vo.getUrl());
        assertTrue(Files.exists(tempDir.resolve(vo.getUrl().substring("/uploads/".length()))));
    }
}
