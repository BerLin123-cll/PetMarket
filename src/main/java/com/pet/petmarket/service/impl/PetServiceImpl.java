package com.pet.petmarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.entity.Category;
import com.pet.petmarket.entity.Pet;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.enums.PetSpecies;
import com.pet.petmarket.mapper.PetMapper;
import com.pet.petmarket.service.CategoryService;
import com.pet.petmarket.service.PetService;
import com.pet.petmarket.service.ProductService;
import com.pet.petmarket.vo.PetVO;
import com.pet.petmarket.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 宠物档案服务实现类
 *
 * <p>包含三块能力：</p>
 * <ol>
 *   <li><b>档案 CRUD</b>：所有写操作都按 userId 校验归属，防止越权操作他人宠物；</li>
 *   <li><b>驱虫提醒</b>：进页面时按「出生日/上次驱虫日 + 驱虫周期」实时推算下次驱虫时间
 *       （幼年 &lt;6 个月每月一次，否则每 3 个月一次），不落库、无定时任务；</li>
 *   <li><b>个性化推荐（方案A）</b>：物种→商品分类映射限定范围 → 排除过敏关键词命中商品
 *       → 按适龄段关键词做优先级排序。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements PetService {

    private final CategoryService categoryService;
    private final ProductService productService;

    /** 推荐结果最多返回的商品数 */
    private static final int RECOMMEND_LIMIT = 8;
    /** 候选商品池上限（在该分类下最多取这么多商品再筛选） */
    private static final int CANDIDATE_LIMIT = 50;
    /** 距下次驱虫 ≤ 该天数即视为「临近/该驱虫了」 */
    private static final int DEWORM_DUE_THRESHOLD_DAYS = 7;

    /** 适龄段关键词：幼年 */
    private static final String[] YOUNG_KEYWORDS = {"幼", "奶糕", "宝宝", "puppy", "kitten"};
    /** 适龄段关键词：成年 */
    private static final String[] ADULT_KEYWORDS = {"成犬", "成猫", "成年", "成"};
    /** 适龄段关键词：老年 */
    private static final String[] SENIOR_KEYWORDS = {"老年", "老犬", "老猫", "senior"};

    @Override
    public List<PetVO> listByUser(Long userId) {
        List<Pet> pets = this.lambdaQuery()
                .eq(Pet::getUserId, userId)
                .orderByDesc(Pet::getCreateTime)
                .list();
        return pets.stream().map(this::toVO).toList();
    }

    @Override
    public PetVO addPet(Long userId, Pet pet) {
        if (pet.getName() == null || pet.getName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "宠物昵称不能为空");
        }
        if (PetSpecies.of(pet.getSpecies()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择正确的宠物物种");
        }
        pet.setId(null);
        pet.setUserId(userId);
        this.save(pet);
        return toVO(pet);
    }

    @Override
    public void updatePet(Long userId, Pet pet) {
        if (pet.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "宠物ID不能为空");
        }
        if (pet.getName() != null && pet.getName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "宠物昵称不能为空");
        }
        if (pet.getSpecies() != null && PetSpecies.of(pet.getSpecies()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择正确的宠物物种");
        }
        getOwnedPet(userId, pet.getId());
        pet.setUserId(userId);
        this.updateById(pet);
    }

    @Override
    public void deletePet(Long userId, Long petId) {
        getOwnedPet(userId, petId);
        this.removeById(petId);
    }

    @Override
    public List<ProductVO> recommendProducts(Long userId, Long petId) {
        Pet pet = getOwnedPet(userId, petId);

        PetSpecies species = PetSpecies.of(pet.getSpecies());
        if (species == null) {
            return Collections.emptyList();
        }

        // 1. 物种 → 商品分类（按分类名匹配，避免硬编码分类ID）
        Category category = categoryService.lambdaQuery()
                .eq(Category::getName, species.getCategoryName())
                .eq(Category::getStatus, 1)
                .last("LIMIT 1")
                .one();
        if (category == null) {
            return Collections.emptyList();
        }

        // 2. 取该分类下的上架商品作为候选池
        List<Product> candidates = productService.lambdaQuery()
                .eq(Product::getStatus, 1)
                .eq(Product::getCategoryId, category.getId())
                .orderByDesc(Product::getCreateTime)
                .last("LIMIT " + CANDIDATE_LIMIT)
                .list();
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 排除命中过敏关键词的商品（过敏词 vs 商品名/描述）
        List<String> allergies = parseAllergies(pet.getAllergyHistory());
        List<Product> filtered = candidates.stream()
                .filter(product -> !containsAny(product, allergies))
                .toList();
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 按适龄段关键词排序（命中优先），其次按上架时间倒序，取前 N 个
        String[] stageKeywords = stageKeywords(pet.getBirthday());
        Comparator<Product> comparator = Comparator
                .comparingInt((Product p) -> containsAny(p, Arrays.asList(stageKeywords)) ? 1 : 0)
                .thenComparing(p -> p.getCreateTime() == null ? LocalDateTime.MIN : p.getCreateTime())
                .reversed();

        return filtered.stream()
                .sorted(comparator)
                .limit(RECOMMEND_LIMIT)
                .map(product -> toProductVO(product, category.getName()))
                .toList();
    }

    // ------------------------------------------------------------------
    // 内部工具方法
    // ------------------------------------------------------------------

    /**
     * 查询归属于当前用户的宠物，不存在或非本人一律按「宠物不存在」处理，
     * 避免暴露他人数据是否存在。
     */
    private Pet getOwnedPet(Long userId, Long petId) {
        Pet pet = petId == null ? null : this.getById(petId);
        if (pet == null || !pet.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PET_NOT_FOUND);
        }
        return pet;
    }

    /**
     * 实体 → VO，并补齐年龄文案与驱虫提醒
     */
    private PetVO toVO(Pet pet) {
        PetVO vo = BeanUtil.copyProperties(pet, PetVO.class);
        vo.setSpeciesLabel(PetSpecies.labelOf(pet.getSpecies()));
        vo.setAgeText(buildAgeText(pet.getBirthday()));
        fillDewormInfo(pet, vo);
        return vo;
    }

    /**
     * 计算年龄文案，如「2岁3个月」「5个月」
     */
    private String buildAgeText(LocalDate birthday) {
        if (birthday == null) {
            return "未知";
        }
        LocalDate today = LocalDate.now();
        if (birthday.isAfter(today)) {
            return "未出生";
        }
        Period period = Period.between(birthday, today);
        if (period.getYears() > 0) {
            return period.getMonths() > 0
                    ? period.getYears() + "岁" + period.getMonths() + "个月"
                    : period.getYears() + "岁";
        }
        long months = period.toTotalMonths();
        if (months > 0) {
            return months + "个月";
        }
        return "不足1个月";
    }

    /**
     * 实时计算驱虫提醒并写入 VO
     *
     * <p>驱虫周期：幼年（&lt;6 个月）每月一次，否则每 3 个月一次。
     * 有「上次驱虫日期」以它为准，否则用生日推算一个首驱锚点。</p>
     */
    private void fillDewormInfo(Pet pet, PetVO vo) {
        LocalDate today = LocalDate.now();
        Long ageMonths = monthsBetween(pet.getBirthday(), today);
        int intervalMonths = (ageMonths != null && ageMonths < 6) ? 1 : 3;

        LocalDate next;
        if (pet.getLastDewormDate() != null) {
            next = pet.getLastDewormDate().plusMonths(intervalMonths);
        } else if (pet.getBirthday() != null) {
            next = pet.getBirthday().plusMonths(intervalMonths);
        } else {
            next = null;
        }

        vo.setNextDewormDate(next);
        if (next == null) {
            vo.setDewormDue(false);
            vo.setDewormTip("登记生日或上次驱虫日期后即可计算驱虫提醒");
            return;
        }

        long daysLeft = ChronoUnit.DAYS.between(today, next);
        vo.setDewormDaysLeft((int) daysLeft);
        boolean due = daysLeft <= DEWORM_DUE_THRESHOLD_DAYS;
        vo.setDewormDue(due);

        if (daysLeft < 0) {
            vo.setDewormTip("已超过建议驱虫日期 " + (-daysLeft) + " 天，请尽快驱虫");
        } else if (daysLeft == 0) {
            vo.setDewormTip("今天建议驱虫");
        } else if (due) {
            vo.setDewormTip("还有 " + daysLeft + " 天建议驱虫");
        } else {
            vo.setDewormTip("距离下次驱虫还有 " + daysLeft + " 天");
        }
    }

    /**
     * 依据生日判断当前适龄段对应的关键词（用于推荐排序）
     */
    private String[] stageKeywords(LocalDate birthday) {
        Long ageMonths = monthsBetween(birthday, LocalDate.now());
        if (ageMonths == null) {
            return new String[0];
        }
        if (ageMonths < 12) {
            return YOUNG_KEYWORDS;
        }
        if (ageMonths < 84) {
            return ADULT_KEYWORDS;
        }
        return SENIOR_KEYWORDS;
    }

    /**
     * 计算出生到今天的整月数，生日为空或晚于今天返回 null
     */
    private Long monthsBetween(LocalDate birthday, LocalDate today) {
        if (birthday == null || birthday.isAfter(today)) {
            return null;
        }
        return Period.between(birthday, today).toTotalMonths();
    }

    /**
     * 解析过敏史文本为关键词列表（支持中英文逗号、顿号、分号、斜杠、空格分隔）
     */
    private List<String> parseAllergies(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(text.split("[,，、;；/\\s]+"))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .toList();
    }

    /**
     * 商品名或描述是否命中任一关键词（忽略大小写）
     */
    private boolean containsAny(Product product, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        String haystack = safe(product.getName()) + " " + safe(product.getDescription());
        haystack = haystack.toLowerCase();
        for (String keyword : keywords) {
            if (!keyword.isEmpty() && haystack.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    /**
     * 商品 → VO（推荐场景下分类名可直接传入，无需再查库）
     */
    private ProductVO toProductVO(Product product, String categoryName) {
        ProductVO vo = BeanUtil.copyProperties(product, ProductVO.class);
        vo.setCategoryName(categoryName);
        return vo;
    }
}
