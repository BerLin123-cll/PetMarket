package com.pet.petmarket.common;

import lombok.Getter;

/**
 * 业务状态码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "账号已被禁用"),
    USERNAME_EXIST(1003, "用户名已存在"),
    PASSWORD_ERROR(1004, "密码错误"),

    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    PRODUCT_OUT_OF_STOCK(2002, "商品库存不足"),
    PRODUCT_OFF_SHELF(2003, "商品已下架"),

    CART_NOT_FOUND(3001, "购物车记录不存在"),

    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态异常"),

    ADDRESS_NOT_FOUND(5001, "地址不存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
