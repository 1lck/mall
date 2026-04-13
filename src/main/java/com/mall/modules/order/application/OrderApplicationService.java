package com.mall.modules.order.application;

import com.mall.modules.order.dto.CreateOrderDTO;
import com.mall.modules.order.vo.OrderVO;
import com.mall.modules.order.dto.UpdateOrderDTO;

import java.util.List;

/**
 * 订单应用服务接口，定义 controller 可以调用的订单能力。
 */
public interface OrderApplicationService {

	OrderVO createOrder(Long currentUserId, CreateOrderDTO request);

	OrderVO getOrder(Long currentUserId, boolean isAdmin, Long id);

	List<OrderVO> listOrders(Long currentUserId, boolean isAdmin);

	OrderVO updateOrder(Long currentUserId, boolean isAdmin, Long id, UpdateOrderDTO request);

	void deleteOrder(Long currentUserId, boolean isAdmin, Long id);
}
