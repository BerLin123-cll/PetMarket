package com.pet.petmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.dto.LoginDTO;
import com.pet.petmarket.entity.Admin;
import com.pet.petmarket.mapper.AdminMapper;
import com.pet.petmarket.service.AdminService;
import com.pet.petmarket.utils.JwtUtils;
import com.pet.petmarket.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final PasswordUtils passwordUtils;
    private final JwtUtils jwtUtils;

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, loginDTO.getUsername());

        Admin admin = this.getOne(wrapper);

        if (admin == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "管理员账号不存在");
        }

        if (!passwordUtils.matches(loginDTO.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String token = jwtUtils.generateToken(admin.getId(), admin.getUsername(), true);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        admin.setPassword(null);
        result.put("admin", admin);

        return result;
    }
}
