package com.pet.petmarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import java.util.List;

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
            throw new RuntimeException(ResultCode.PRODUCT_NOT_FOUND.getMessage());
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException(ResultCode.PRODUCT_OUT_OF_STOCK.getMessage());
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
    public void updateCartQuantity(Long cartId, Integer quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("数量必须大于0");
        }

        Cart cart = this.getById(cartId);
        if (cart == null) {
            throw new RuntimeException(ResultCode.CART_NOT_FOUND.getMessage());
        }

        Product product = productService.getById(cart.getProductId());
        if (product == null || product.getStock() < quantity) {
            throw new RuntimeException(ResultCode.PRODUCT_OUT_OF_STOCK.getMessage());
        }

        cart.setQuantity(quantity);
        this.updateById(cart);
    }

    @Override
    public void removeFromCart(Long cartId) {
        if (cartId == null) {
            throw new RuntimeException(ResultCode.CART_NOT_FOUND.getMessage());
        }
        this.removeById(cartId);
    }

    @Override
    public List<CartVO> getUserCartList(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);

        List<Cart> carts = this.list(wrapper);

        return carts.stream().map(cart -> {
            CartVO vo = BeanUtil.copyProperties(cart, CartVO.class);

            Product product = productService.getById(cart.getProductId());
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
