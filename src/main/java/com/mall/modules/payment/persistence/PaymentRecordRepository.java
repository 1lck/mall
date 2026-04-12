package com.mall.modules.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 支付记录仓储。
 */
public interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, Long> {

	Optional<PaymentRecordEntity> findByOrderNo(String orderNo);
}
