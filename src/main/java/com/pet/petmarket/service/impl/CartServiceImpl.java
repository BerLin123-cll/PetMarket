package com.pet.petmarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.entity.Cart;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.mapper.CartMapper;
import com.pet.petmarket.service.CartService;
import com.pet.petmarket.service.ProductService;
import com.pet.petmarket.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务实现类
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final ProductService productService;

    @Override
    public void addToCart(Long userId, Long productId, Integer quantity) {
        Product product = productService.getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        if (product.getStock() < quantity) {
            throw new BusinessException(ResultCode.PRODUCT_OUT_OF_STOCK);
        }

        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .eq(Cart::getProductId, productId);

        Cart existCart = this.getOne(wrapper);

        if (existCart != null) {
            existCart.setQuantity(existCart.getQuantity() + quantity);
            this.updateById(existCart);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            this.save(cart);
        }
    }

    @Override
    public void updateCartQuantity(Long userId, Long cartId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "数量必须大于0");
        }

        // 校验购物车归属，防止越权修改他人购物车项
        Cart cart = getOwnedCart(userId, cartId);

        Product product = productService.getById(cart.getProductId());
        if (product == null || product.getStock() < quantity) {
            throw new BusinessException(ResultCode.PRODUCT_OUT_OF_STOCK);
        }

        cart.setQuantity(quantity);
        this.updateById(cart);
    }

    @Override
    public void removeFromCart(Long userId, Long cartId) {
        // 校验购物车归属，防止越权删除他人购物车项
        getOwnedCart(userId, cartId);
        this.removeById(cartId);
    }

    /**
     * 查询归属于当前用户的购物车项。
     *
     * <p>无论“不存在”还是“属于其他用户”，都统一抛 {@code CART_NOT_FOUND}，
     * 避免通过错误提示探测他人购物车数据是否存在。</p>
     */
    private Cart getOwnedCart(Long userId, Long cartId) {
        Cart cart = cartId == null ? null : this.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CART_NOT_FOUND);
        }
        return cart;
    }

    @Override
    public List<CartVO> getUserCartList(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);

        List<Cart> carts = this.list(wrapper);
        if (carts.isEmpty()) {
            return Collections.emptyList();
        }

        // 一次性批量查出购物车涉及的所有商品，避免逐条 getById 造成 N+1 查询
        List<Long> productIds = carts.stream().map(Cart::getProductId).distinct().toList();
        Map<Long, Product> productMap = productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return carts.stream().map(cart -> {
            CartVO vo = BeanUtil.copyProperties(cart, CartVO.class);

            Product product = productMap.get(cart.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductImage(product.getMainImage());
                vo.setPrice(product.getPrice());
                vo.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            }

            return vo;
        }).toList();
    }

    @Override
    public void clearUserCart(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        this.remove(wrapper);
    }
}
