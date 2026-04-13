package com.mall.modules.product.vo;

/**
 * 商品图片上传成功后的返回体。
 */
public record ProductImageUploadVO(
	String objectKey,
	String imageUrl
) {
}
