package com.pet.petmarket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.petmarket.entity.Pet;
import com.pet.petmarket.vo.PetVO;
import com.pet.petmarket.vo.ProductVO;

import java.util.List;

/**
 * 宠物档案服务接口
 */
public interface PetService extends IService<Pet> {

    /**
     * 查询当前用户的宠物列表（含年龄、驱虫提醒等派生信息）
     */
    List<PetVO> listByUser(Long userId);

    /**
     * 新增宠物档案
     */
    PetVO addPet(Long userId, Pet pet);

    /**
     * 更新宠物档案（校验归属，防止越权改他人宠物）
     */
    void updatePet(Long userId, Pet pet);

    /**
     * 删除宠物档案（校验归属）
     */
    void deletePet(Long userId, Long petId);

    /**
     * 为指定宠物做个性化商品推荐
     *
     * <p>规则（方案A）：按「物种→商品分类」映射限定范围，排除命中过敏关键词的商品，
     * 再按「适龄段」关键词做优先级排序。</p>
     *
     * @return 推荐商品列表（最多 8 个，无合适商品时返回空列表）
     */
    List<ProductVO> recommendProducts(Long userId, Long petId);
}
