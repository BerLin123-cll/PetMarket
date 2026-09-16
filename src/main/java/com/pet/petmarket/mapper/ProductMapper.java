package com.pet.petmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.vo.LowStockProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品数据访问层
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 条件扣减库存（防超卖的原子操作）
     *
     * <p>在 SQL 的 WHERE 中直接判断库存是否充足，由数据库行锁保证并发安全：
     * 两个并发下单请求中，只有一个能更新成功（返回 1），另一个返回 0，
     * 从而彻底避免「先查询后判断再修改」造成的超卖。</p>
     *
     * @param productId 商品ID
     * @param quantity  扣减数量（必须 > 0）
     * @return 影响行数：1=扣减成功，0=库存不足或商品不存在
     */
    @Update("UPDATE product SET stock = stock - #{quantity}, update_time = NOW() " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 回补库存（取消订单 / 超时释放时调用）
     *
     * @param productId 商品ID
     * @param quantity  回补数量（必须 > 0）
     * @return 影响行数：1=回补成功，0=商品不存在（可能已被删除）
     */
    @Update("UPDATE product SET stock = stock + #{quantity}, update_time = NOW() " +
            "WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 低库存预警：筛出上架中且库存 ≤ 阈值的商品，库存最少者优先
     *
     * @param threshold 库存阈值（含）
     * @param limit     返回条数
     */
    @Select("SELECT id, name, stock, price FROM product " +
            "WHERE status = 1 AND stock <= #{threshold} ORDER BY stock ASC, id ASC LIMIT #{limit}")
    List<LowStockProductVO> selectLowStock(@Param("threshold") int threshold, @Param("limit") int limit);
}
