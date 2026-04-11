package com.mall.modules.product.application;

import com.mall.modules.product.api.CreateProductRequest;
import com.mall.modules.product.api.ProductResponse;
import com.mall.modules.product.api.UpdateProductRequest;

import java.util.List;

/**
 * 商品应用服务接口。
 */
public interface ProductApplicationService {

	ProductResponse createProduct(CreateProductRequest request);

	ProductResponse getProduct(Long id);

	List<ProductResponse> listProducts();

	ProductResponse updateProduct(Long id, UpdateProductRequest request);

	void deleteProduct(Long id);
}
