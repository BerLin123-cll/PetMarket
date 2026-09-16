package com.pet.petmarket.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.entity.User;
import com.pet.petmarket.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器 - 后台管理端
 */
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {
    
    private final UserService userService;
    
    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public Result<IPage<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        
        Page<User> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        
        IPage<User> userPage = userService.page(page, wrapper);
        
        // 隐藏密码字段
        userPage.getRecords().forEach(user -> user.setPassword(null));
        
        return Result.success(userPage);
    }
    
    /**
     * 禁用/启用用户账号
     */
    @PostMapping("/toggleStatus/{userId}")
    public Result<Void> toggleUserStatus(@PathVariable Long userId, @RequestBody ToggleStatusRequest req) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(req.getStatus());
        userService.updateById(user);
        return Result.success();
    }
    
    @Data
    static class ToggleStatusRequest {
        private Integer status;
    }
}
