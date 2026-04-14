package com.mall.modules.product.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.product.dto.CreateProductDTO;
import com.mall.modules.product.dto.UpdateProductDTO;
import com.mall.modules.product.domain.ProductStatus;
import com.mall.modules.product.persistence.entity.ProductEntity;
import com.mall.modules.product.persistence.mapper.ProductMapper;
import com.mall.modules.product.vo.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 商品应用服务，负责承接控制器请求并协调仓储读写。
 */
@Service
@Transactional
public class DefaultProductApplicationService implements ProductApplicationService {

	private static final DateTimeFormatter PRODUCT_NO_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT).withZone(ZoneOffset.UTC);

	private final ProductMapper productRepository;

	public DefaultProductApplicationService(ProductMapper productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * 创建商品。
	 */
	@Override
	public ProductVO createProduct(CreateProductDTO request) {
		// 第一版先直接在服务层组装商品对象，后续拆更复杂的领域逻辑也方便演进。
		ProductEntity product = new ProductEntity();
		product.setProductNo(generateProductNo());
		product.setName(request.name());
		product.setCategoryName(request.categoryName());
		product.setPrice(request.price());
		product.setStock(request.stock());
		product.setStatus(ProductStatus.DRAFT);
		product.setDescription(request.description());
		product.setImageUrl(request.imageUrl());

		return toResponse(productRepository.save(product));
	}

	/**
	 * 读取单个商品详情。
	 */
	@Override
	@Transactional(readOnly = true)
	public ProductVO getProduct(Long id) {
		return toResponse(getProductEntity(id));
	}

	/**
	 * 返回商品列表。
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ProductVO> listProducts() {
		// 列表按 id 倒序返回，方便前端先看到最新创建的商品。
		return productRepository.findAllByOrderByIdDesc()
			.stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * 更新商品可编辑字段，并校验状态流转是否合法。
	 */
	@Override
	public ProductVO updateProduct(Long id, UpdateProductDTO request) {
		// 更新前先查原商品，找不到就直接抛业务异常。
		ProductEntity product = getProductEntity(id);
		validateStatusTransition(product.getStatus(), request.status());
		product.setName(request.name());
		product.setCategoryName(request.categoryName());
		product.setPrice(request.price());
		product.setStock(request.stock());
		product.setStatus(request.status());
		product.setDescription(request.description());
		product.setImageUrl(request.imageUrl());

		return toResponse(productRepository.save(product));
	}

	/**
	 * 删除指定商品。
	 */
	@Override
	public void deleteProduct(Long id) {
		// 当前先做硬删除，后续如果需要回收站再改成软删除。
		ProductEntity product = getProductEntity(id);
		productRepository.delete(product);
	}

	/**
	 * 读取商品实体，并统一处理商品不存在的情况。
	 */
	private ProductEntity getProductEntity(Long id) {
		// 把“查不到商品”的判断收口到一个地方，避免每个方法重复判空。
		return productRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Product " + id + " was not found"));
	}

	/**
	 * 校验商品状态是否允许流转到目标状态。
	 */
	private void validateStatusTransition(ProductStatus currentStatus, ProductStatus targetStatus) {
		// 状态流转规则统一收口在服务层，避免接口层直接把任意状态写进数据库。
		if (!currentStatus.canTransitionTo(targetStatus)) {
			throw new BusinessException(
				ErrorCode.BAD_REQUEST,
				"Product status cannot transition from " + currentStatus + " to " + targetStatus
			);
		}
	}

	/**
	 * 把商品实体转换成接口响应对象。
	 */
	private ProductVO toResponse(ProductEntity product) {
		// persistence 层对象不直接返回给前端，这里统一转换成接口响应对象。
		return new ProductVO(
			product.getId(),
			product.getProductNo(),
			product.getName(),
			product.getCategoryName(),
			product.getPrice(),
			product.getStock(),
			product.getStatus(),
			product.getDescription(),
			product.getImageUrl(),
			product.getCreatedAt(),
			product.getUpdatedAt()
		);
	}

	/**
	 * 生成商品编号。
	 */
	private String generateProductNo() {
		// 用时间戳加随机片段生成商品编号，当前练手项目场景已经够用。
		return "PRD" + PRODUCT_NO_TIME_FORMATTER.format(Instant.now())
			+ UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
	}
}
