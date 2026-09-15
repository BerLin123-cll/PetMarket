package com.pet.petmarket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.petmarket.dto.LoginDTO;
import com.pet.petmarket.dto.RegisterDTO;
import com.pet.petmarket.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    User register(RegisterDTO registerDTO);

    /**
     * 用户登录
     * @return 返回 Token 和用户信息
     */
    Map<String, Object> login(LoginDTO loginDTO);

    /**
     * 获取用户信息
     */
    User getUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    void updateUserInfo(Long userId, User user);
}
