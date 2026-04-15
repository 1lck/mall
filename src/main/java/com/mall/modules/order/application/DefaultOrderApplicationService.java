package com.mall.modules.order.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.order.dto.CreateOrderDTO;
import com.mall.modules.order.vo.OrderVO;
import com.mall.modules.order.dto.UpdateOrderDTO;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.order.persistence.entity.OrderEntity;
import com.mall.modules.order.persistence.mapper.OrderMapper;
import com.mall.modules.product.persistence.entity.ProductEntity;
import com.mall.modules.product.persistence.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单应用服务，负责承接控制器请求并协调仓储读写。
 */
@Service
@Transactional
public class DefaultOrderApplicationService implements OrderApplicationService {

	private static final DateTimeFormatter ORDER_NO_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT).withZone(ZoneOffset.UTC);

	private final OrderMapper orderRepository;
	private final ProductMapper productRepository;
	private final OrderEventPublisher orderEventPublisher;

	public DefaultOrderApplicationService(
		OrderMapper orderRepository,
		ProductMapper productRepository,
		OrderEventPublisher orderEventPublisher
	) {
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.orderEventPublisher = orderEventPublisher;
	}

	/**
	 * 创建订单，并在订单持久化成功后发布订单创建事件。
	 */
	@Override
	public OrderVO createOrder(Long currentUserId, CreateOrderDTO request) {
		// 下单主流程：
		// 1. 找到商品
		// 2. 校验库存
		// 3. 计算订单金额
		// 4. 扣减商品库存
		// 5. 创建订单
		// 6. 发布订单创建事件
		// 这里仍然是“下单”的核心入口。
		// 第一阶段先保留同步落库逻辑，再在落库成功后补一条订单创建事件。
		ProductEntity product = getProductEntity(request.productId());
		int quantity = request.quantity();
		validateStock(product, quantity);
		BigDecimal totalAmount = calculateOrderAmount(product.getPrice(), quantity);

		OrderEntity order = new OrderEntity();
		order.setOrderNo(generateOrderNo());
		order.setUserId(currentUserId);
		order.setProductId(product.getId());
		order.setQuantity(quantity);
		order.setTotalAmount(totalAmount);
		order.setStatus(OrderStatus.CREATED);
		order.setRemark(request.remark());

		int remainingStock = product.getStock() - quantity;
		product.setStock(remainingStock);
		productRepository.save(product);

		// save(order) 的意义就是把这条订单真正写入 orders 表。
		// 保存后返回的对象通常会带上数据库生成的 id、时间戳等完整信息，
		// 所以后面发消息和返回前端时都优先使用 savedOrder。
		OrderEntity savedOrder = orderRepository.save(order);

		// 第一版先做最简单的事件发布：
		// 订单已经成功入库 -> 发布 OrderCreatedEvent。
		// 这样后续库存、支付、超时取消等异步流程都能从这条消息开始扩展。
		orderEventPublisher.publishOrderCreated(toOrderCreatedEvent(savedOrder));
		return toResponse(savedOrder);
	}

	/**
	 * 读取单笔订单详情，并按当前用户权限控制可见范围。
	 */
	@Override
	@Transactional(readOnly = true)
	public OrderVO getOrder(Long currentUserId, boolean isAdmin, Long id) {
		// 按订单 id 读取单条订单，并在这里统一处理“管理员可看全部、普通用户只能看自己”的权限差异。
		return toResponse(getOrderEntity(currentUserId, isAdmin, id));
	}

	/**
	 * 查询订单列表，管理员可见全部订单，普通用户仅可见自己的订单。
	 */
	@Override
	@Transactional(readOnly = true)
	public List<OrderVO> listOrders(Long currentUserId, boolean isAdmin) {
		// 读取订单列表：
		// 管理员看全部订单，普通用户只看自己的订单。
		List<OrderEntity> orders = isAdmin
			? orderRepository.findAllByOrderByIdDesc()
			: orderRepository.findAllByUserIdOrderByIdDesc(currentUserId);

		// 列表按 id 倒序返回，方便前端先看到最新创建的订单。
		return orders
			.stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * 更新订单可编辑字段，并校验状态流转是否合法。
	 */
	@Override
	public OrderVO updateOrder(Long currentUserId, boolean isAdmin, Long id, UpdateOrderDTO request) {
		// 更新订单入口只开放给管理员，避免普通用户自行改金额或伪造支付状态。
		OrderEntity order = getOrderEntity(currentUserId, isAdmin, id);
		validateUpdatePermission(isAdmin);
		validateManualPaidTransition(order.getStatus(), request.status());
		validateStatusTransition(order.getStatus(), request.status());
		order.setTotalAmount(request.totalAmount());
		order.setStatus(request.status());
		order.setRemark(request.remark());

		return toResponse(orderRepository.save(order));
	}

	/**
	 * 删除指定订单。
	 */
	@Override
	public void deleteOrder(Long currentUserId, boolean isAdmin, Long id) {
		// 删除订单入口，目前是硬删除，权限控制仍然复用统一的订单读取逻辑。
		// 当前先做硬删除，后面如果需要保留历史可以改成软删除。
		OrderEntity order = getOrderEntity(currentUserId, isAdmin, id);
		orderRepository.delete(order);
	}

	/**
	 * 按权限读取订单实体，并统一处理订单不存在的情况。
	 */
	private OrderEntity getOrderEntity(Long currentUserId, boolean isAdmin, Long id) {
		// 统一的订单读取方法：
		// 同时收口“按权限查订单”和“订单不存在时抛业务异常”这两件事。
		// 把“查不到订单”的判断收口在一个地方，避免重复写判空逻辑。
		Optional<OrderEntity> orderOptional = isAdmin
			? orderRepository.findById(id)
			: orderRepository.findByIdAndUserId(id, currentUserId);

		if (orderOptional.isEmpty()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "Order " + id + " was not found");
		}

		return orderOptional.get();
	}

	/**
	 * 校验订单状态是否允许流转到目标状态。
	 */
	private void validateStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
		// 订单状态机校验：
		// 防止接口层把不合法的状态直接写进数据库。
		// 状态流转规则统一收口在服务层，避免不同入口各写一套判断。
		if (!currentStatus.canTransitionTo(targetStatus)) {
			throw new BusinessException(
				ErrorCode.BAD_REQUEST,
				"Order status cannot transition from " + currentStatus + " to " + targetStatus
			);
		}
	}

	/**
	 * 校验当前请求是否具备修改订单的权限。
	 */
	private void validateUpdatePermission(boolean isAdmin) {
		if (!isAdmin) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号没有权限更新订单。");
		}
	}

	/**
	 * 阻止接口层直接把订单流转成已支付，避免绕过支付成功链路。
	 */
	private void validateManualPaidTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
		if (currentStatus != targetStatus && targetStatus == OrderStatus.PAID) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "订单支付状态只能由支付流程更新。");
		}
	}

	/**
	 * 把订单实体转换成接口响应对象。
	 */
	private OrderVO toResponse(OrderEntity order) {
		// 把数据库实体转换成接口响应对象，避免 controller 直接暴露 persistence 层模型。
		// persistence 层对象不直接返回给前端，这里统一转换成接口响应对象。
		return new OrderVO(
			order.getId(),
			order.getOrderNo(),
			order.getUserId(),
			order.getTotalAmount(),
			order.getProductId(),
			order.getQuantity(),
			order.getStatus(),
			order.getRemark(),
			order.getCreatedAt(),
			order.getUpdatedAt()
		);
	}

	/**
	 * 读取下单所需的商品实体，并统一处理商品不存在的场景。
	 */
	private ProductEntity getProductEntity(Long productId) {
		// 统一的商品读取方法：
		// 下单时只要依赖商品，就都从这里处理“商品不存在”的判断。
		Optional<ProductEntity> productOptional = productRepository.findById(productId);
		if (productOptional.isEmpty()) {
			throw new BusinessException(
				ErrorCode.NOT_FOUND,
				"Product " + productId + " was not found"
			);
		}

		return productOptional.get();
	}

	/**
	 * 校验库存是否足够支持当前下单数量。
	 */
	private void validateStock(ProductEntity product, Integer quantity) {
		// 库存校验：
		// 当前同步扣减版本里，下单前先确保库存足够，库存不足就直接失败。
		if (product.getStock() < quantity) {
			throw new BusinessException(
				ErrorCode.BAD_REQUEST,
				"Product " + product.getId() + " stock is insufficient"
			);
		}
	}

	/**
	 * 计算订单总金额。
	 */
	private BigDecimal calculateOrderAmount(BigDecimal productPrice, Integer quantity) {
		// 订单金额计算：
		// 当前最小版本按“商品单价 * 数量”计算总金额。
		return productPrice.multiply(BigDecimal.valueOf(quantity.longValue()));
	}

	/**
	 * 把订单实体转换成订单创建事件。
	 */
	private OrderCreatedEvent toOrderCreatedEvent(OrderEntity order) {
		// 把订单实体转换成消息事件对象，给后续 Kafka 链路使用。
		// 这里把数据库实体转换成“发给 Kafka 的事件对象”。
		// 目的就是把“数据库模型”和“消息模型”分开，后面即使表结构变化，
		// 也不一定要把消息格式一起改乱。
		return new OrderCreatedEvent(
			order.getId(),
			order.getOrderNo(),
			order.getUserId(),
			order.getTotalAmount(),
			order.getStatus().name(),
			order.getCreatedAt()
		);
	}

	/**
	 * 生成订单编号。
	 */
	private String generateOrderNo() {
		// 订单号生成器：
		// 用时间戳 + 随机片段生成当前项目里够用的业务单号。
		// 用时间戳加随机片段生成一个足够练手项目使用的订单号。
		return "ORD" + ORDER_NO_TIME_FORMATTER.format(Instant.now())
			+ UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
	}
}
