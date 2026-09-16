package com.pet.petmarket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 宠物档案展示 VO
 *
 * <p>在实体字段基础上补充「年龄文案」与「驱虫提醒」等由后端实时计算出来的派生字段，
 * 前端直接展示即可，无需自己算日期。</p>
 */
@Data
public class PetVO {

    private Long id;

    private String name;

    /** 物种编码 */
    private Integer species;

    /** 物种展示名，如「狗」「猫」 */
    private String speciesLabel;

    private String breed;

    /** 性别：0-未知，1-公，2-母 */
    private Integer gender;

    private LocalDate birthday;

    /** 年龄文案，如「2岁3个月」「5个月」，生日为空时为「未知」 */
    private String ageText;

    private BigDecimal weight;

    private String allergyHistory;

    private String avatar;

    /** 上次驱虫日期 */
    private LocalDate lastDewormDate;

    /** 下次建议驱虫日期（为空表示信息不足无法计算） */
    private LocalDate nextDewormDate;

    /** 距下次驱虫剩余天数，负数表示已过期 */
    private Integer dewormDaysLeft;

    /** 是否临近（7 天内）或已到驱虫时间 */
    private Boolean dewormDue;

    /** 驱虫提醒文案 */
    private String dewormTip;
}
