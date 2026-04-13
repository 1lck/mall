package com.mall.modules.product;

import com.mall.common.exception.GlobalExceptionHandler;
import com.mall.modules.product.vo.ProductImageUploadVO;
import com.mall.modules.product.application.ProductImageApplicationService;
import com.mall.modules.product.controller.ProductImageController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品图片上传接口测试，验证控制器能正确接收文件并返回统一响应结构。
 */
@WebMvcTest(ProductImageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ProductImageUploadControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProductImageApplicationService productImageApplicationService;

	@Test
	void uploadProductImageShouldReturnImageUrl() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"iphone16.png",
			"image/png",
			"fake-image-content".getBytes()
		);

		given(productImageApplicationService.uploadImage(file)).willReturn(
			new ProductImageUploadVO(
				"product-images/2026/04/11/test-file.png",
				"http://localhost:9000/mall-product-images/product-images/2026/04/11/test-file.png"
			)
		);

		// 上传成功后，接口应该把图片地址和对象 key 一起返回给前端。
		mockMvc.perform(multipart("/api/v1/products/images/upload").file(file).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.objectKey").value("product-images/2026/04/11/test-file.png"))
			.andExpect(jsonPath("$.data.imageUrl")
				.value("http://localhost:9000/mall-product-images/product-images/2026/04/11/test-file.png"));
	}
}
