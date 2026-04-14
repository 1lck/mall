package com.mall.modules.order.application;

import com.mall.modules.order.dto.CreateOrderDTO;
import com.mall.modules.order.vo.OrderVO;
import com.mall.modules.order.dto.UpdateOrderDTO;

import java.util.List;

/**
 * 订单应用服务接口，定义 controller 可以调用的订单能力。
 */
public interface OrderApplicationService {

	/**
	 * 创建一笔新订单。
	 */
	OrderVO createOrder(Long currentUserId, CreateOrderDTO request);

	/**
	 * 读取指定订单详情。
	 */
	OrderVO getOrder(Long currentUserId, boolean isAdmin, Long id);

	/**
	 * 根据权限返回订单列表。
	 */
	List<OrderVO> listOrders(Long currentUserId, boolean isAdmin);

	/**
	 * 更新订单的可编辑字段。
	 */
	OrderVO updateOrder(Long currentUserId, boolean isAdmin, Long id, UpdateOrderDTO request);

	/**
	 * 删除指定订单。
	 */
	void deleteOrder(Long currentUserId, boolean isAdmin, Long id);
}
