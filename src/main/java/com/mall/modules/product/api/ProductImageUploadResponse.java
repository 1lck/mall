package com.mall.modules.product.api;

/**
 * 商品图片上传成功后的返回体。
 */
public record ProductImageUploadResponse(
	String objectKey,
	String imageUrl
) {
}
