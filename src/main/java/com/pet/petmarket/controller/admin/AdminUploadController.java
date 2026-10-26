package com.pet.petmarket.controller.admin;

import com.pet.petmarket.common.Result;
import com.pet.petmarket.config.AdminGuardInterceptor;
import com.pet.petmarket.service.FileUploadService;
import com.pet.petmarket.vo.UploadVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 上传商品图片
     *
     * <p>请求示例（前端已封装）：</p>
     * <pre>
     * POST /admin/upload/image
     * Content-Type: multipart/form-data
     * Authorization: Bearer &lt;adminToken&gt;
     * file: &lt;图片二进制&gt;
     * bizType: product（可选，avatar / category / other）
     * </pre>
     *
     * @return {@code { url: "/uploads/product/20250101/xxx.jpg", name: "原文件名", size: 12345 }}
     */
    @PostMapping("/image")
    public Result<UploadVO> uploadImage(HttpServletRequest request,
                                        @RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "bizType", defaultValue = FileUploadService.BIZ_PRODUCT) String bizType) {
        Long adminId = (Long) request.getAttribute(AdminGuardInterceptor.ATTR_ADMIN_ID);
        UploadVO vo = fileUploadService.uploadImage(file, bizType, adminId);
        return Result.success(vo);
    }
}
