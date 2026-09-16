package com.pet.petmarket.controller.api;

import com.pet.petmarket.common.Result;
import com.pet.petmarket.entity.Pet;
import com.pet.petmarket.service.PetService;
import com.pet.petmarket.vo.PetVO;
import com.pet.petmarket.vo.ProductVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 宠物档案相关接口（前台，需登录）
 *
 * <p>/api/** 已被 {@code AuthInterceptor} 拦截，userId 从 Token 中解析后放入请求属性。</p>
 */
@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /**
     * 我的宠物列表（含年龄、驱虫提醒）
     */
    @GetMapping("/list")
    public Result<List<PetVO>> listPets(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(petService.listByUser(userId));
    }

    /**
     * 新增宠物档案
     */
    @PostMapping("/add")
    public Result<PetVO> addPet(HttpServletRequest request, @RequestBody Pet pet) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(petService.addPet(userId, pet));
    }

    /**
     * 更新宠物档案
     */
    @PutMapping("/update/{id}")
    public Result<Void> updatePet(HttpServletRequest request, @PathVariable Long id, @RequestBody Pet pet) {
        Long userId = (Long) request.getAttribute("userId");
        pet.setId(id);
        petService.updatePet(userId, pet);
        return Result.success();
    }

    /**
     * 删除宠物档案
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePet(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        petService.deletePet(userId, id);
        return Result.success();
    }

    /**
     * 为指定宠物推荐商品（依据物种 + 年龄 + 过敏史）
     */
    @GetMapping("/{id}/recommend")
    public Result<List<ProductVO>> recommend(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(petService.recommendProducts(userId, id));
    }
}
