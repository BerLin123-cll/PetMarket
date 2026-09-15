package com.pet.petmarket.controller.api;

import com.pet.petmarket.common.Result;
import com.pet.petmarket.service.CartService;
import com.pet.petmarket.vo.CartVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车相关接口（前台）
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    
    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<Void> addToCart(HttpServletRequest request, @RequestBody AddCartRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.addToCart(userId, req.getProductId(), req.getQuantity());
        return Result.success();
    }
    
    /**
     * 更新购物车商品数量
     */
    @PutMapping("/update/{id}")
    public Result<Void> updateCartQuantity(@PathVariable Long id, @RequestBody UpdateQuantityRequest req) {
        cartService.updateCartQuantity(id, req.getQuantity());
        return Result.success();
    }
    
    /**
     * 删除购物车项
     */
    @DeleteMapping("/remove/{id}")
    public Result<Void> removeFromCart(@PathVariable Long id) {
        cartService.removeFromCart(id);
        return Result.success();
    }
    
    /**
     * 获取用户购物车列表
     */
    @GetMapping("/list")
    public Result<List<CartVO>> getUserCartList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<CartVO> carts = cartService.getUserCartList(userId);
        return Result.success(carts);
    }
    
    /**
     * 清空用户购物车
     */
    @DeleteMapping("/clear")
    public Result<Void> clearUserCart(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.clearUserCart(userId);
        return Result.success();
    }
    
    @Data
    static class AddCartRequest {
        private Long productId;
        private Integer quantity = 1;
    }
    
    @Data
    static class UpdateQuantityRequest {
        private Integer quantity;
    }
}
