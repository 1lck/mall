package com.mall.modules.product.controller;

import com.mall.common.api.ApiResponse;
import com.mall.modules.product.vo.ProductImageUploadVO;
import com.mall.modules.product.application.ProductImageApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品图片上传接口。
 */
@RestController
@RequestMapping("/api/v1/products/images")
@Tag(name = "Product Image", description = "Product image upload endpoints")
public class ProductImageController {

	private final ProductImageApplicationService productImageApplicationService;

	public ProductImageController(ProductImageApplicationService productImageApplicationService) {
		this.productImageApplicationService = productImageApplicationService;
	}

	@PostMapping("/upload")
	@Operation(summary = "Upload product image", description = "Uploads an image to MinIO and returns the public URL.")
	public ApiResponse<ProductImageUploadVO> uploadProductImage(@RequestParam("file") MultipartFile file) {
		// 先返回对象 key 和图片地址，前端后面创建商品时可以直接复用这个 URL。
		return ApiResponse.success(productImageApplicationService.uploadImage(file));
	}
}
