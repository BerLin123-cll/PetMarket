package com.pet.petmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上传文件记录表
 *
 * <p>图片文件本身存放在本机磁盘（{@code petmarket.upload.dir}），
 * 数据库只记录元信息（访问地址、原始文件名、大小、分类、上传人），
 * 便于后台查看与后续清理，也方便将来平滑迁移到 OSS / 对象存储。</p>
 */
@Data
@TableName("upload_file")
public class UploadFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务分类：product-商品图片，avatar-用户头像，category-分类图标，other-其他 */
    private String bizType;

    /** 原始文件名 */
    private String originalName;

    /** 服务端存储文件名（UUID，避免同名的覆盖与路径探测） */
    private String storedName;

    /** 磁盘相对路径，如 product/20250101/uuid.jpg */
    private String relativePath;

    /** 对外访问 URL，如 /uploads/product/20250101/uuid.jpg */
    private String url;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 内容类型（MIME） */
    private String contentType;

    /** 上传用户ID（管理员或普通用户，取决于上传入口） */
    private Long uploaderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
