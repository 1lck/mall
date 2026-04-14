package com.mall.modules.product.application;

import com.mall.modules.product.dto.CreateProductDTO;
import com.mall.modules.product.vo.ProductVO;
import com.mall.modules.product.dto.UpdateProductDTO;

import java.util.List;

/**
 * 商品应用服务接口。
 */
public interface ProductApplicationService {

	/**
	 * 创建商品。
	 */
	ProductVO createProduct(CreateProductDTO request);

	/**
	 * 读取单个商品详情。
	 */
	ProductVO getProduct(Long id);

	/**
	 * 返回商品列表。
	 */
	List<ProductVO> listProducts();

	/**
	 * 更新商品可编辑字段。
	 */
	ProductVO updateProduct(Long id, UpdateProductDTO request);

	/**
	 * 删除指定商品。
	 */
	void deleteProduct(Long id);
}
