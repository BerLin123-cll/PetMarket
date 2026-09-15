package com.pet.petmarket.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadVO {

    /** 可直接用于 &lt;img src&gt; 的访问地址，如 /uploads/product/20250101/xxx.jpg */
    private String url;

    /** 原始文件名 */
    private String name;

    /** 文件大小（字节） */
    private Long size;
}
