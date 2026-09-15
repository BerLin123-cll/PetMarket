package com.pet.petmarket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.petmarket.dto.LoginDTO;
import com.pet.petmarket.entity.Admin;

import java.util.Map;

/**
 * 管理员服务接口
 */
public interface AdminService extends IService<Admin> {

    /**
     * 管理员登录
     */
    Map<String, Object> login(LoginDTO loginDTO);
}
