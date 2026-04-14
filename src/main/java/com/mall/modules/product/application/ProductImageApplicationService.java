package com.mall.modules.product.application;

import com.mall.modules.product.vo.ProductImageUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品图片应用服务接口。
 */
public interface ProductImageApplicationService {

	/**
	 * 上传商品图片并返回可访问地址。
	 */
	ProductImageUploadVO uploadImage(MultipartFile file);
}
