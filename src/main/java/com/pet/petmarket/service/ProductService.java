package com.pet.petmarket.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.petmarket.dto.ProductQueryDTO;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.vo.ProductVO;

/**
 * 商品服务接口
 */
public interface ProductService extends IService<Product> {

    /**
     * 分页查询商品列表
     */
    IPage<ProductVO> getProductPage(ProductQueryDTO queryDTO);

    /**
     * 获取商品详情
     */
    ProductVO getProductDetail(Long productId);

    /**
     * 首页热门商品推荐
     */
    IPage<ProductVO> getHotProducts(Integer pageNum, Integer pageSize);

    /**
     * 创建商品
     */
    void createProduct(Product product);

    /**
     * 更新商品
     */
    void updateProduct(Product product);

    /**
     * 删除商品
     */
    void deleteProduct(Long productId);

    /**
     * 上下架商品
     */
    void toggleProductStatus(Long productId, Integer status);
}
