package com.mall.modules.product.application;

import com.mall.modules.product.dto.CreateProductDTO;
import com.mall.modules.product.vo.ProductVO;
import com.mall.modules.product.dto.UpdateProductDTO;

import java.util.List;

/**
 * 商品应用服务接口。
 */
public interface ProductApplicationService {

	ProductVO createProduct(CreateProductDTO request);

	ProductVO getProduct(Long id);

	List<ProductVO> listProducts();

	ProductVO updateProduct(Long id, UpdateProductDTO request);

	void deleteProduct(Long id);
}
