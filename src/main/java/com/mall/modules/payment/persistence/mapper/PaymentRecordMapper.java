package com.mall.modules.payment.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.modules.payment.persistence.entity.PaymentRecordEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * 支付记录 Mapper。
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecordEntity> {

	default PaymentRecordEntity save(PaymentRecordEntity entity) {
		if (entity.getId() == null) {
			insert(entity);
		} else {
			updateById(entity);
		}

		return entity;
	}

	default Optional<PaymentRecordEntity> findByOrderNo(String orderNo) {
		return Optional.ofNullable(selectOne(
			Wrappers.<PaymentRecordEntity>lambdaQuery().eq(PaymentRecordEntity::getOrderNo, orderNo)
		));
	}
}
