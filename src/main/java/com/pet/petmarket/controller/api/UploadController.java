package com.pet.petmarket.controller.api;

import com.pet.petmarket.common.Result;
import com.pet.petmarket.service.FileUploadService;
import com.pet.petmarket.vo.UploadVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 前台文件上传接口（登录用户，如头像上传）
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileUploadService fileUploadService;

    /**
     * 上传用户头像（需登录）
     */
    @PostMapping("/avatar")
    public Result<UploadVO> uploadAvatar(HttpServletRequest request,
                                         @RequestParam("file") MultipartFile file) {
        Long userId = (Long) request.getAttribute("userId");
        UploadVO vo = fileUploadService.uploadImage(file, FileUploadService.BIZ_AVATAR, userId);
        return Result.success(vo);
    }
}
