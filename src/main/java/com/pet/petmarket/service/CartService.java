package com.pet.petmarket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.petmarket.entity.Cart;
import com.pet.petmarket.vo.CartVO;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService extends IService<Cart> {

    /**
     * 添加商品到购物车
     */
    void addToCart(Long userId, Long productId, Integer quantity);

    /**
     * 更新购物车商品数量
     *
     * @param userId   当前登录用户ID（用于校验购物车归属，防止越权操作他人数据）
     * @param cartId   购物车项ID
     * @param quantity 目标数量
     */
    void updateCartQuantity(Long userId, Long cartId, Integer quantity);

    /**
     * 删除购物车项
     *
     * @param userId 当前登录用户ID（用于校验购物车归属，防止越权删除他人数据）
     * @param cartId 购物车项ID
     */
    void removeFromCart(Long userId, Long cartId);

    /**
     * 获取用户购物车列表
     */
    List<CartVO> getUserCartList(Long userId);

    /**
     * 清空用户购物车
     */
    void clearUserCart(Long userId);
}
