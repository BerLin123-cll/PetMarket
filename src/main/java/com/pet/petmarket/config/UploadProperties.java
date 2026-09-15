package com.pet.petmarket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地图片上传配置
 *
 * <p>对应 application.yml 中的 {@code petmarket.upload.*}，支持通过环境变量覆盖，
 * 便于后续切换到 OSS / 服务器部署时只改配置不改代码。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "petmarket.upload")
public class UploadProperties {

    /** 上传文件保存的本地根目录（相对项目运行目录或绝对路径），默认 ./data/upload */
    private String dir = "./data/upload";

    /** 静态资源访问前缀，默认 /uploads */
    private String urlPrefix = "/uploads";

    /** 单文件大小上限（字节），默认 5MB */
    private long maxSize = 5 * 1024 * 1024;

    /** 允许上传的扩展名（小写，含点） */
    private List<String> allowedExtensions = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    /** 允许上传的 MIME 类型 */
    private List<String> allowedContentTypes = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/x-ms-bmp"
    );

    /** 商品图片的子目录（相对于 dir） */
    private String productSubDir = "product";

    /** 是否校验图片文件的真实魔数（防止改扩展名上传非法文件） */
    private boolean checkMagicNumber = true;
}
