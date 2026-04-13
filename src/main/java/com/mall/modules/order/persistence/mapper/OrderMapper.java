package com.mall.modules.order.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.modules.order.persistence.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 订单 Mapper。
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

	default OrderEntity save(OrderEntity entity) {
		if (entity.getId() == null) {
			insert(entity);
		} else {
			updateById(entity);
		}

		return entity;
	}

	default Optional<OrderEntity> findById(Long id) {
		return Optional.ofNullable(selectById(id));
	}

	default Optional<OrderEntity> findByIdAndUserId(Long id, Long userId) {
		return Optional.ofNullable(selectOne(
			Wrappers.<OrderEntity>lambdaQuery()
				.eq(OrderEntity::getId, id)
				.eq(OrderEntity::getUserId, userId)
		));
	}

	default Optional<OrderEntity> findByOrderNo(String orderNo) {
		return Optional.ofNullable(selectOne(
			Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getOrderNo, orderNo)
		));
	}

	default List<OrderEntity> findAll() {
		return selectList(null);
	}

	default List<OrderEntity> findAllByOrderByIdDesc() {
		return selectList(Wrappers.<OrderEntity>lambdaQuery().orderByDesc(OrderEntity::getId));
	}

	default List<OrderEntity> findAllByUserIdOrderByIdDesc(Long userId) {
		return selectList(
			Wrappers.<OrderEntity>lambdaQuery()
				.eq(OrderEntity::getUserId, userId)
				.orderByDesc(OrderEntity::getId)
		);
	}

	default void delete(OrderEntity entity) {
		deleteById(entity.getId());
	}
}
