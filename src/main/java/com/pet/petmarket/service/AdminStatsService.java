package com.pet.petmarket.service;

import com.pet.petmarket.vo.HotProductVO;
import com.pet.petmarket.vo.LowStockProductVO;
import com.pet.petmarket.vo.OrderStatusCountVO;
import com.pet.petmarket.vo.RepurchaseVO;
import com.pet.petmarket.vo.SalesTrendVO;

import java.util.List;

/**
 * 后台数据概览统计服务
 *
 * <p>为管理端 Dashboard 提供 5 类经营洞察数据：销售趋势、订单状态漏斗、
 * 热销商品、低库存预警、用户复购率。取数全部走 SQL 聚合，不在内存中循环全表。</p>
 */
public interface AdminStatsService {

    /**
     * 销售趋势：近 {@code days} 天每日销售额（已支付订单），缺失日期补 0
     *
     * @param days 统计天数，为空或非法时取默认值 7，最大 30
     */
    List<SalesTrendVO> getSalesTrend(Integer days);

    /**
     * 订单状态漏斗：各状态订单数量，缺失状态补 0，按状态顺序返回
     */
    List<OrderStatusCountVO> getOrderStatusFunnel();

    /**
     * 热销商品：已支付订单中销量最高的前 {@code limit} 个商品
     *
     * @param limit 返回条数，为空或非法时取默认值 5，最大 50
     */
    List<HotProductVO> getHotProducts(Integer limit);

    /**
     * 低库存预警：上架中且库存 ≤ 阈值的商品
     *
     * @param threshold 库存阈值（含），为空或负数时取默认值 10
     * @param limit     返回条数，为空或非法时取默认值 20，最大 100
     */
    List<LowStockProductVO> getLowStockProducts(Integer threshold, Integer limit);

    /**
     * 用户复购率：有支付记录的用户中下单 ≥2 笔的占比
     */
    RepurchaseVO getRepurchaseRate();
}
