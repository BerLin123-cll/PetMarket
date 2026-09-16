package com.pet.petmarket.vo;

import lombok.Data;

/**
 * 订单状态分布 VO
 *
 * <p>后台数据概览「订单状态漏斗」用：按订单状态分组计数，
 * 服务层会补齐缺失状态并写入 {@code statusText}。</p>
 */
@Data
public class OrderStatusCountVO {

    /** 订单状态编码：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消 */
    private Integer status;

    /** 状态展示名 */
    private String statusText;

    /** 该状态的订单数量 */
    private Long count;
}
