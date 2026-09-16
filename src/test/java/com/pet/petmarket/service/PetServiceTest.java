package com.pet.petmarket.service;

import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.entity.Category;
import com.pet.petmarket.entity.Pet;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.mapper.CategoryMapper;
import com.pet.petmarket.mapper.PetMapper;
import com.pet.petmarket.mapper.ProductMapper;
import com.pet.petmarket.vo.PetVO;
import com.pet.petmarket.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 宠物档案（P2）核心逻辑测试
 *
 * <p>覆盖点：</p>
 * <ol>
 *   <li>档案 CRUD：新增后可查、物种/年龄文案补齐，列表按用户隔离；</li>
 *   <li>归属校验：改/删/推荐他人宠物一律按「宠物不存在」拒绝，防越权；</li>
 *   <li>入参校验：昵称必填、物种合法；</li>
 *   <li>驱虫提醒：实时按「上次驱虫日/生日 + 驱虫周期」推算，幼年每月、成年每 3 个月；</li>
 *   <li>个性化推荐：物种→分类限定范围、过敏词排除、适龄段关键词优先排序。</li>
 * </ol>
 *
 * <p>使用独立 H2 内存库（MySQL 兼容模式），不依赖本机 MySQL。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:petmarket_pet;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class PetServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;

    @Autowired
    private PetService petService;
    @Autowired
    private PetMapper petMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;

    private Long dogCategoryId;

    @BeforeEach
    void setUp() {
        petMapper.delete(null);
        productMapper.delete(null);
        categoryMapper.delete(null);

        dogCategoryId = insertCategory("狗狗用品", 1);
        insertCategory("猫咪用品", 2);
    }

    // ---------------------------------------------------------------
    // 测试辅助
    // ---------------------------------------------------------------

    private Long insertCategory(String name, int sort) {
        Category category = new Category();
        category.setName(name);
        category.setSortOrder(sort);
        category.setStatus(1);
        categoryMapper.insert(category);
        return category.getId();
    }

    private Long insertProduct(String name, String description, Long categoryId) {
        Product product = new Product();
        product.setName(name);
        product.setCategoryId(categoryId);
        product.setPrice(new BigDecimal("50.00"));
        product.setStock(10);
        product.setDescription(description);
        product.setStatus(1);
        productMapper.insert(product);
        return product.getId();
    }

    private Long addPet(Long userId, String name, Integer species,
                        LocalDate birthday, String allergy, LocalDate lastDeworm) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBirthday(birthday);
        pet.setAllergyHistory(allergy);
        pet.setLastDewormDate(lastDeworm);
        return petService.addPet(userId, pet).getId();
    }

    private static boolean hasName(List<ProductVO> list, String keyword) {
        return list.stream().anyMatch(vo -> vo.getName() != null && vo.getName().contains(keyword));
    }

    // ---------------------------------------------------------------
    // 1. 新增 + 列表（含派生字段）+ 用户隔离
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收1：新增宠物后可查，补齐物种名与年龄文案，且列表按用户隔离")
    void addPetThenListShowsDerivedFields() {
        addPet(USER_ID, "旺财", 1, LocalDate.now().minusMonths(30), null, null);

        List<PetVO> mine = petService.listByUser(USER_ID);
        assertEquals(1, mine.size());
        PetVO vo = mine.get(0);
        assertEquals("狗", vo.getSpeciesLabel(), "应补齐物种展示名");
        assertNotEquals("未知", vo.getAgeText(), "有生日时应算出年龄文案");
        assertTrue(vo.getAgeText().contains("岁"), "30 个月应展示为含「岁」的文案：" + vo.getAgeText());
        assertEquals(USER_ID, petMapper.selectById(vo.getId()).getUserId(), "档案归属应为当前用户");

        assertTrue(petService.listByUser(OTHER_USER_ID).isEmpty(), "不应看到他人宠物");
    }

    // ---------------------------------------------------------------
    // 2. 入参校验
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收2：昵称为空、物种非法时新增失败")
    void addPetValidatesInput() {
        Pet blankName = new Pet();
        blankName.setSpecies(1);
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> petService.addPet(USER_ID, blankName));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), e1.getCode());

        Pet badSpecies = new Pet();
        badSpecies.setName("小可爱");
        badSpecies.setSpecies(99);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> petService.addPet(USER_ID, badSpecies));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), e2.getCode());
    }

    // ---------------------------------------------------------------
    // 3. 归属校验：改/删/推荐他人宠物一律拒绝
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收3：更新/删除/推荐他人宠物均按「宠物不存在」拒绝")
    void ownershipIsEnforced() {
        Long petId = addPet(USER_ID, "旺财", 1, null, null, null);

        Pet update = new Pet();
        update.setId(petId);
        update.setName("被篡改");
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> petService.updatePet(OTHER_USER_ID, update));
        assertEquals(ResultCode.PET_NOT_FOUND.getCode(), e1.getCode());
        assertEquals("旺财", petMapper.selectById(petId).getName(), "越权更新不得生效");

        BusinessException e2 = assertThrows(BusinessException.class,
                () -> petService.deletePet(OTHER_USER_ID, petId));
        assertEquals(ResultCode.PET_NOT_FOUND.getCode(), e2.getCode());
        assertNotNull(petMapper.selectById(petId), "越权删除不得生效");

        BusinessException e3 = assertThrows(BusinessException.class,
                () -> petService.recommendProducts(OTHER_USER_ID, petId));
        assertEquals(ResultCode.PET_NOT_FOUND.getCode(), e3.getCode());

        // 本人删除应成功
        petService.deletePet(USER_ID, petId);
        assertNull(petMapper.selectById(petId));
    }

    // ---------------------------------------------------------------
    // 4. 驱虫提醒：幼年（<6 月）周期为 1 个月
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收4：幼年宠物按「上次驱虫 + 1 个月」推算，到期则提醒")
    void dewormForYoungPetDueToday() {
        // 2 个月大（<6 月 → 周期 1 个月），上次驱虫在 1 个月前 → 下次恰为今天
        Long petId = addPet(USER_ID, "奶狗", 1,
                LocalDate.now().minusMonths(2), null, LocalDate.now().minusMonths(1));

        PetVO vo = petService.listByUser(USER_ID).get(0);
        assertEquals(LocalDate.now(), vo.getNextDewormDate(), "幼年周期为 1 个月");
        assertEquals(0, vo.getDewormDaysLeft());
        assertTrue(vo.getDewormDue(), "当天应视为该驱虫");
        assertTrue(vo.getDewormTip().contains("今天"), vo.getDewormTip());
        assertNotNull(petId);
    }

    // ---------------------------------------------------------------
    // 5. 驱虫提醒：成年（>=6 月）周期为 3 个月，未到期不提醒
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收5：成年宠物按「上次驱虫 + 3 个月」推算，未到期则只展示剩余天数")
    void dewormForAdultNotDue() {
        // 3 岁（>=6 月 → 周期 3 个月），上次驱虫在 5 天前 → 下次约 80 天后
        addPet(USER_ID, "成年犬", 1,
                LocalDate.now().minusYears(3), null, LocalDate.now().minusDays(5));

        PetVO vo = petService.listByUser(USER_ID).get(0);
        assertEquals(LocalDate.now().minusDays(5).plusMonths(3), vo.getNextDewormDate());
        assertTrue(vo.getDewormDaysLeft() > 7, "剩余天数应远大于阈值");
        assertFalse(vo.getDewormDue(), "未到期不应触发提醒");
        assertTrue(vo.getDewormTip().contains("距离下次驱虫还有"), vo.getDewormTip());
    }

    // ---------------------------------------------------------------
    // 6. 驱虫提醒：信息不足时给出引导文案而非报错
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收6：生日与上次驱虫日期皆空时给出引导文案")
    void dewormWithoutEnoughInfo() {
        addPet(USER_ID, "未知生日", 2, null, null, null);

        PetVO vo = petService.listByUser(USER_ID).get(0);
        assertNull(vo.getNextDewormDate());
        assertFalse(vo.getDewormDue());
        assertTrue(vo.getDewormTip().contains("登记"), vo.getDewormTip());
    }

    // ---------------------------------------------------------------
    // 7. 个性化推荐：过敏排除 + 适龄段优先
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收7：推荐排除命中过敏词的狗粮，并优先展示适龄段商品")
    void recommendExcludesAllergyAndPrioritizesAgeStage() {
        insertProduct("成犬鸡肉味狗粮", "全价成犬粮，含鸡肉", dogCategoryId);
        insertProduct("幼犬奶糕", "适合幼犬的奶糕", dogCategoryId);
        insertProduct("狗狗牵引绳", "反光牵引绳", dogCategoryId);

        // 3 个月大的狗（幼年），对鸡肉过敏
        Long petId = addPet(USER_ID, "幼犬", 1,
                LocalDate.now().minusMonths(3), "鸡肉", null);

        List<ProductVO> recommend = petService.recommendProducts(USER_ID, petId);

        assertEquals(2, recommend.size(), "鸡肉味狗粮应被过敏词排除");
        assertFalse(hasName(recommend, "鸡肉"), "推荐结果不得包含命中过敏词的商品");
        assertTrue(hasName(recommend, "幼犬奶糕"), "应包含适龄商品");
        assertTrue(recommend.get(0).getName().contains("幼"), "幼年宠物应优先展示幼年关键词商品");
    }

    // ---------------------------------------------------------------
    // 8. 个性化推荐：物种无对应商品时返回空列表
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收8：该物种分类下无上架商品时返回空列表")
    void recommendReturnsEmptyWhenNoProduct() {
        // 猫咪用品分类存在但没有任何商品
        Long petId = addPet(USER_ID, "喵喵", 2, null, null, null);

        assertTrue(petService.recommendProducts(USER_ID, petId).isEmpty());
    }
}
