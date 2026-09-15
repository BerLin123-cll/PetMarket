package com.pet.petmarket.service;

import com.pet.petmarket.vo.UploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件上传服务接口
 *
 * <p>当前阶段（暂不做服务器部署）图片统一保存在本机磁盘，由应用直接对外提供静态访问。</p>
 */
public interface FileUploadService {

    /** 业务分类：商品图片 */
    String BIZ_PRODUCT = "product";
    /** 业务分类：用户头像 */
    String BIZ_AVATAR = "avatar";
    /** 业务分类：分类图标 */
    String BIZ_CATEGORY = "category";

    /**
     * 保存上传的图片到本机磁盘
     *
     * @param file       上传的文件
     * @param bizType    业务分类（决定子目录），如 {@link #BIZ_PRODUCT}
     * @param uploaderId 上传人ID，可为 null
     * @return 上传结果（访问地址、原始文件名、大小）
     */
    UploadVO uploadImage(MultipartFile file, String bizType, Long uploaderId);
}
