package com.pet.petmarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.dto.LoginDTO;
import com.pet.petmarket.dto.RegisterDTO;
import com.pet.petmarket.entity.User;
import com.pet.petmarket.mapper.UserMapper;
import com.pet.petmarket.service.UserService;
import com.pet.petmarket.utils.JwtUtils;
import com.pet.petmarket.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordUtils passwordUtils;
    private final JwtUtils jwtUtils;

    @Override
    public User register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException(ResultCode.USERNAME_EXIST.getMessage());
        }

        // 创建用户对象
        User user = new User();
        BeanUtil.copyProperties(registerDTO, user);

        // 密码加密存储
        user.setPassword(passwordUtils.encode(registerDTO.getPassword()));
        user.setStatus(1);

        this.save(user);

        // 隐藏密码后返回
        user.setPassword(null);
        return user;
    }

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(wrapper);

        if (user == null) {
            throw new RuntimeException(ResultCode.USER_NOT_FOUND.getMessage());
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new RuntimeException(ResultCode.USER_DISABLED.getMessage());
        }

        // 验证密码
        if (!passwordUtils.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException(ResultCode.PASSWORD_ERROR.getMessage());
        }

        // 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), false);

        // 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        user.setPassword(null);
        result.put("user", user);

        return result;
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public void updateUserInfo(Long userId, User user) {
        user.setId(userId);
        user.setPassword(null);
        this.updateById(user);
    }
}
