package com.mall.modules.product.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.product.api.CreateProductRequest;
import com.mall.modules.product.api.ProductResponse;
import com.mall.modules.product.api.UpdateProductRequest;
import com.mall.modules.product.application.ProductApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品 CRUD 接口，先提供最小可用版本，后续再扩展搜索、类目和图片信息。
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "Product CRUD endpoints")
public class ProductController {

	private final ProductApplicationService productApplicationService;

	public ProductController(ProductApplicationService productApplicationService) {
		this.productApplicationService = productApplicationService;
	}

	@PostMapping
	@Operation(summary = "Create product", description = "Creates a new product and returns the persisted result.")
	public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
		@Valid @RequestBody CreateProductRequest request
	) {
		// 创建接口单独返回 201，更符合 REST 语义。
		return ResponseEntity.status(201)
			.body(ApiResponse.success(productApplicationService.createProduct(request)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get product by id", description = "Reads a single product by its primary key.")
	public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
		return ApiResponse.success(productApplicationService.getProduct(id));
	}

	@GetMapping
	@Operation(summary = "List products", description = "Returns all products ordered by id descending.")
	public ApiResponse<List<ProductResponse>> listProducts() {
		return ApiResponse.success(productApplicationService.listProducts());
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update product", description = "Updates editable fields of an existing product.")
	public ApiResponse<ProductResponse> updateProduct(
		@PathVariable Long id,
		@Valid @RequestBody UpdateProductRequest request
	) {
		return ApiResponse.success(productApplicationService.updateProduct(id, request));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete product", description = "Deletes a product by id.")
	public ApiResponse<String> deleteProduct(@PathVariable Long id) {
		productApplicationService.deleteProduct(id);
		// 当前删除接口只返回一个简单确认结果。
		return ApiResponse.success("deleted");
	}
}
