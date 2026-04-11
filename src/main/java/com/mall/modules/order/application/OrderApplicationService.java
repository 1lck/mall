package com.mall.modules.order.application;

import com.mall.modules.order.api.CreateOrderRequest;
import com.mall.modules.order.api.OrderResponse;
import com.mall.modules.order.api.UpdateOrderRequest;

import java.util.List;

/**
 * 订单应用服务接口，定义 controller 可以调用的订单能力。
 */
public interface OrderApplicationService {

	OrderResponse createOrder(Long currentUserId, CreateOrderRequest request);

	OrderResponse getOrder(Long currentUserId, boolean isAdmin, Long id);

	List<OrderResponse> listOrders(Long currentUserId, boolean isAdmin);

	OrderResponse updateOrder(Long currentUserId, boolean isAdmin, Long id, UpdateOrderRequest request);

	void deleteOrder(Long currentUserId, boolean isAdmin, Long id);
}
