package com.mall.modules.order.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.modules.order.persistence.entity.OrderEventRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单事件消费记录仓储。
 *
 * <p>这里不再走“先查有没有处理过，再决定是否继续”的两步式写法，
 * 而是直接把“抢处理资格”下沉到数据库里原子完成。</p>
 */
@Mapper
public interface OrderEventRecordMapper extends BaseMapper<OrderEventRecordEntity> {

	default OrderEventRecordEntity save(OrderEventRecordEntity entity) {
		if (entity.getId() == null) {
			insert(entity);
		} else {
			updateById(entity);
		}

		return entity;
	}

	/**
	 * 原子抢占某个订单事件的处理资格。
	 *
	 * <p>返回 1 表示当前消费者抢占成功，可以继续执行业务副作用；
	 * 返回 0 表示已有其他消费者抢先处理，当前消费者应直接跳过。</p>
	 *
	 * <p>借助数据库唯一约束和 {@code on conflict do nothing}，
	 * 可以把“谁先处理”这件事交给数据库裁决，减少并发窗口。</p>
	 */
	@Insert("""
		insert into order_event_records (event_type, order_no, processed_at)
		values (#{eventType}, #{orderNo}, current_timestamp)
		on conflict (event_type, order_no) do nothing
		""")
	int claimProcessing(@Param("eventType") String eventType, @Param("orderNo") String orderNo);
}
