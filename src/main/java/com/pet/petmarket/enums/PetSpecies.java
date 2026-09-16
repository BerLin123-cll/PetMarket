package com.pet.petmarket.enums;

import lombok.Getter;

/**
 * 宠物物种枚举
 *
 * <p>除了给前端展示 label，还承载「物种 → 商品分类」的映射关系：
 * 个性化推荐时按该映射找到对应分类，再在该分类下筛选商品。
 * 分类名称与 {@code category.name} 保持一致（如「狗狗用品」）。</p>
 */
@Getter
public enum PetSpecies {

    DOG(1, "狗", "狗狗用品"),
    CAT(2, "猫", "猫咪用品"),
    SMALL_PET(3, "小宠", "小宠用品"),
    REPTILE(4, "爬宠", "爬宠用品"),
    BIRD(5, "鸟", "鸟类宠物"),
    FISH(6, "鱼", "水族宠物");

    /** 物种编码（与数据库 species 字段对应） */
    private final Integer code;

    /** 展示名称 */
    private final String label;

    /** 推荐时对应的商品分类名称 */
    private final String categoryName;

    PetSpecies(Integer code, String label, String categoryName) {
        this.code = code;
        this.label = label;
        this.categoryName = categoryName;
    }

    /**
     * 根据物种编码查找枚举，找不到返回 null
     */
    public static PetSpecies of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PetSpecies species : values()) {
            if (species.code.equals(code)) {
                return species;
            }
        }
        return null;
    }

    /**
     * 根据物种编码取展示名称，找不到返回「未知」
     */
    public static String labelOf(Integer code) {
        PetSpecies species = of(code);
        return species == null ? "未知" : species.label;
    }
}
