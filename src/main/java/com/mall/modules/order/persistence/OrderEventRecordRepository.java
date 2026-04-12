package com.mall.modules.order.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
