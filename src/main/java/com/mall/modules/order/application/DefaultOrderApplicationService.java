package com.mall.modules.order.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.order.api.CreateOrderRequest;
import com.mall.modules.order.api.OrderResponse;
import com.mall.modules.order.api.UpdateOrderRequest;
import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.persistence.OrderEntity;
import com.mall.modules.order.persistence.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 订单应用服务，负责承接控制器请求并协调仓储读写。
 */
@Service
@Transactional
public class DefaultOrderApplicationService implements OrderApplicationService {

	private static final DateTimeFormatter ORDER_NO_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT).withZone(ZoneOffset.UTC);

	private final OrderRepository orderRepository;

	public DefaultOrderApplicationService(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@Override
	public OrderResponse createOrder(Long currentUserId, CreateOrderRequest request) {
		// 第一版先在服务层组装订单对象，后面如果接事件流也会从这里扩展。
		OrderEntity order = new OrderEntity();
		order.setOrderNo(generateOrderNo());
		order.setUserId(currentUserId);
		order.setTotalAmount(request.totalAmount());
		order.setStatus(OrderStatus.CREATED);
		order.setRemark(request.remark());

		return toResponse(orderRepository.save(order));
	}

	@Override
	@Transactional(readOnly = true)
	public OrderResponse getOrder(Long currentUserId, boolean isAdmin, Long id) {
		return toResponse(getOrderEntity(currentUserId, isAdmin, id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<OrderResponse> listOrders(Long currentUserId, boolean isAdmin) {
		List<OrderEntity> orders = isAdmin
			? orderRepository.findAllByOrderByIdDesc()
			: orderRepository.findAllByUserIdOrderByIdDesc(currentUserId);

		// 列表按 id 倒序返回，方便前端先看到最新创建的订单。
		return orders
			.stream()
			.map(this::toResponse)
			.toList();
	}

	@Override
	public OrderResponse updateOrder(Long currentUserId, boolean isAdmin, Long id, UpdateOrderRequest request) {
		// 更新时先查原订单，找不到就直接抛业务异常。
		OrderEntity order = getOrderEntity(currentUserId, isAdmin, id);
		validateStatusTransition(order.getStatus(), request.status());
		order.setTotalAmount(request.totalAmount());
		order.setStatus(request.status());
		order.setRemark(request.remark());

		return toResponse(orderRepository.save(order));
	}

	@Override
	public void deleteOrder(Long currentUserId, boolean isAdmin, Long id) {
		// 当前先做硬删除，后面如果需要保留历史可以改成软删除。
		OrderEntity order = getOrderEntity(currentUserId, isAdmin, id);
		orderRepository.delete(order);
	}

	private OrderEntity getOrderEntity(Long currentUserId, boolean isAdmin, Long id) {
		// 把“查不到订单”的判断收口在一个地方，避免重复写判空逻辑。
		return (isAdmin ? orderRepository.findById(id) : orderRepository.findByIdAndUserId(id, currentUserId))
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order " + id + " was not found"));
	}

	private void validateStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
		// 状态流转规则统一收口在服务层，避免不同入口各写一套判断。
		if (!currentStatus.canTransitionTo(targetStatus)) {
			throw new BusinessException(
				ErrorCode.BAD_REQUEST,
				"Order status cannot transition from " + currentStatus + " to " + targetStatus
			);
		}
	}

	private OrderResponse toResponse(OrderEntity order) {
		// persistence 层对象不直接返回给前端，这里统一转换成接口响应对象。
		return new OrderResponse(
			order.getId(),
			order.getOrderNo(),
			order.getUserId(),
			order.getTotalAmount(),
			order.getStatus(),
			order.getRemark(),
			order.getCreatedAt(),
			order.getUpdatedAt()
		);
	}

	private String generateOrderNo() {
		// 用时间戳加随机片段生成一个足够练手项目使用的订单号。
		return "ORD" + ORDER_NO_TIME_FORMATTER.format(Instant.now())
			+ UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
	}
}
