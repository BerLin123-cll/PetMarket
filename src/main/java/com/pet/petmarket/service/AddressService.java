package com.pet.petmarket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.petmarket.entity.Address;

import java.util.List;

/**
 * 收货地址服务接口
 */
public interface AddressService extends IService<Address> {

    /**
     * 新增收货地址
     */
    void addAddress(Long userId, Address address);

    /**
     * 更新收货地址
     */
    void updateAddress(Long userId, Address address);

    /**
     * 删除收货地址
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 获取用户地址列表
     */
    List<Address> getUserAddresses(Long userId);

    /**
     * 设置默认地址
     */
    void setDefaultAddress(Long userId, Long addressId);

    /**
     * 获取用户默认地址
     */
    Address getDefaultAddress(Long userId);
}
