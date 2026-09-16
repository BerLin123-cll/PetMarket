package com.pet.petmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.entity.Address;
import com.pet.petmarket.mapper.AddressMapper;
import com.pet.petmarket.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务实现类
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    public void addAddress(Long userId, Address address) {
        address.setUserId(userId);

        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Address::getUserId, userId)
                    .set(Address::getIsDefault, 0);
            this.update(wrapper);
        }

        this.save(address);
    }

    @Override
    public void updateAddress(Long userId, Address address) {
        if (address.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "地址ID不能为空");
        }

        Address existAddress = this.getById(address.getId());
        if (existAddress == null || !existAddress.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Address::getUserId, userId)
                    .ne(Address::getId, address.getId())
                    .set(Address::getIsDefault, 0);
            this.update(wrapper);
        }

        this.updateById(address);
    }


    @Override
    public void deleteAddress(Long userId, Long addressId) {
        Address address = this.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }
        this.removeById(addressId);
    }

    @Override
    public List<Address> getUserAddresses(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .orderByDesc(Address::getIsDefault)
               .orderByDesc(Address::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        Address address = this.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .set(Address::getIsDefault, 0);
        this.update(wrapper);

        address.setIsDefault(1);
        this.updateById(address);
    }

    @Override
    public Address getDefaultAddress(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .eq(Address::getIsDefault, 1);
        return this.getOne(wrapper);
    }
}
