package com.pet.petmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.petmarket.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单数据访问层
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    /**
     * 条件流转订单状态（保证同一订单只会被释放一次库存）
     *
     * <p>WHERE 中带上原状态，配合数据库行锁实现「状态机 CAS」：
     * 定时任务与用户手动取消即使同时执行，也只有一方能更新成功（返回 1），
     * 另一方返回 0，从而保证库存不会被重复回补。</p>
     *
     * @param orderId    订单ID
     * @param fromStatus 原状态（必须匹配）
     * @param toStatus   目标状态
     * @return 影响行数：1=本次调用成功流转，0=已被其他线程处理
     */
    @Update("UPDATE orders SET order_status = #{toStatus} WHERE id = #{orderId} AND order_status = #{fromStatus}")
    int updateStatusIfMatch(@Param("orderId") Long orderId,
                            @Param("fromStatus") Integer fromStatus,
                            @Param("toStatus") Integer toStatus);

    /**
     * 条件更新支付状态（防止重复支付 / 重复回调）
     *
     * @param orderId    订单ID
     * @param fromStatus 原订单状态
     * @param toStatus   目标订单状态
     * @param payTime    支付时间
     * @return 影响行数：1=支付成功，0=订单状态已变更（重复支付或已超时取消）
     */
    @Update("UPDATE orders SET order_status = #{toStatus}, pay_status = 1, pay_time = #{payTime} " +
            "WHERE id = #{orderId} AND order_status = #{fromStatus} AND pay_status = 0")
    int markPaidIfUnpaid(@Param("orderId") Long orderId,
                         @Param("fromStatus") Integer fromStatus,
                         @Param("toStatus") Integer toStatus,
                         @Param("payTime") LocalDateTime payTime);

    /**
     * 查询已超时未支付的订单ID列表（按创建时间升序，优先处理最早的订单）
     *
     * @param deadline 超时截止时间（创建时间早于该值即视为超时）
     * @param limit    单批最大处理数量，避免一次扫描过多订单
     */
    @Select("SELECT id FROM orders WHERE order_status = 0 AND pay_status = 0 " +
            "AND create_time <= #{deadline} ORDER BY create_time ASC LIMIT #{limit}")
    List<Long> selectTimeoutOrderIds(@Param("deadline") LocalDateTime deadline,
                                     @Param("limit") int limit);

    /**
     * 统计已支付订单的销售总额
     *
     * <p>直接在数据库用 {@code SUM} 聚合，避免把全部已支付订单加载到内存再累加。
     * {@code COALESCE} 保证无记录时返回 0 而不是 NULL。</p>
     *
     * @return 销售总额
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE pay_status = 1")
    BigDecimal sumPaidAmount();
}
