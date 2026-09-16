package com.pet.petmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物档案实体类
 *
 * <p>用户登记自己的宠物（品种、生日、体重、过敏史等），
 * 用于「个性化商品推荐」与「驱虫提醒」。</p>
 */
@Data
@TableName("pet")
public class Pet {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主人用户ID */
    private Long userId;

    /** 宠物昵称 */
    private String name;

    /** 物种：1-狗，2-猫，3-小宠，4-爬宠，5-鸟，6-鱼 */
    private Integer species;

    /** 品种 */
    private String breed;

    /** 性别：0-未知，1-公，2-母 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 体重(kg) */
    private BigDecimal weight;

    /** 过敏史（逗号/顿号分隔的关键词，如：鸡肉,牛肉） */
    private String allergyHistory;

    /** 宠物头像URL */
    private String avatar;

    /** 上次驱虫日期（为空时按生日推算首次驱虫） */
    private LocalDate lastDewormDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
