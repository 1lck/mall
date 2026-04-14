package com.mall.modules.product.vo;

/**
 * 商品图片上传成功后的返回体。
 */
public record ProductImageUploadVO(
	/** 对象存储中的 object key。 */
	String objectKey,
	/** 可直接访问的图片地址。 */
	String imageUrl
) {
}
