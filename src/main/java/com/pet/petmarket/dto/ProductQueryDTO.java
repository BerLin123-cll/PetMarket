package com.pet.petmarket.dto;

import lombok.Data;

/**
 * 商品查询条件 DTO
 */
@Data
public class ProductQueryDTO {
    
    private Long categoryId;
    
    private String keyword;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
    
    private String sortField;
    
    private String sortOrder;
}
