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
     */
    void updateCartQuantity(Long cartId, Integer quantity);

    /**
     * 删除购物车项
     */
    void removeFromCart(Long cartId);

    /**
     * 获取用户购物车列表
     */
    List<CartVO> getUserCartList(Long userId);

    /**
     * 清空用户购物车
     */
    void clearUserCart(Long userId);
}
