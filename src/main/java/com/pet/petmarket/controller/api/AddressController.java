package com.pet.petmarket.controller.api;

import com.pet.petmarket.common.Result;
import com.pet.petmarket.entity.Address;
import com.pet.petmarket.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址相关接口（前台）
 */
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {
    
    private final AddressService addressService;
    
    /**
     * 新增收货地址
     */
    @PostMapping("/add")
    public Result<Void> addAddress(HttpServletRequest request, @RequestBody Address address) {
        Long userId = (Long) request.getAttribute("userId");
        addressService.addAddress(userId, address);
        return Result.success();
    }
    
    /**
     * 更新收货地址
     */
    @PutMapping("/update/{id}")
    public Result<Void> updateAddress(HttpServletRequest request, @PathVariable Long id, @RequestBody Address address) {
        Long userId = (Long) request.getAttribute("userId");
        address.setId(id);
        addressService.updateAddress(userId, address);
        return Result.success();
    }
    
    /**
     * 删除收货地址
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteAddress(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        addressService.deleteAddress(userId, id);
        return Result.success();
    }
    
    /**
     * 获取用户地址列表
     */
    @GetMapping("/list")
    public Result<List<Address>> getUserAddresses(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Address> addresses = addressService.getUserAddresses(userId);
        return Result.success(addresses);
    }
    
    /**
     * 设置默认地址
     */
    @PutMapping("/setDefault/{id}")
    public Result<Void> setDefaultAddress(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        addressService.setDefaultAddress(userId, id);
        return Result.success();
    }
}
