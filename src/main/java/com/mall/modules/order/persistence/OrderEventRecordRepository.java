package com.mall.modules.order.persistence;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 订单事件消费记录仓储。
 *
 * <p>第一阶段先只用到最基础的 save 能力，
 * 也就是把消费者处理过的事件记录保存到 order_event_records 表里。</p>
 *
 * <p>后面如果要做幂等、防重复消费，
 * 可以继续在这里补 existsBy... 之类的查询方法。</p>
 */
public interface OrderEventRecordRepository extends JpaRepository<OrderEventRecordEntity, Long> {

	/**
	 * 原子抢占某个订单事件的处理资格。
	 *
	 * <p>返回 1 表示当前消费者抢占成功，可以继续执行业务副作用；
	 * 返回 0 表示已有其他消费者抢先处理，当前消费者应直接跳过。</p>
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(
		value = """
			insert into order_event_records (event_type, order_no, processed_at)
			values (:eventType, :orderNo, current_timestamp)
			on conflict (event_type, order_no) do nothing
			""",
		nativeQuery = true
	)
	int claimProcessing(@Param("eventType") String eventType, @Param("orderNo") String orderNo);
}
