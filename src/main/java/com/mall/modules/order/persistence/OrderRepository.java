package com.mall.modules.order.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 订单仓储，负责基础的数据库增删改查。
 */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

	Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

	List<OrderEntity> findAllByOrderByIdDesc();

	List<OrderEntity> findAllByUserIdOrderByIdDesc(Long userId);
}
