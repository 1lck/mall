package com.mall.modules.outbox.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbox 事件仓储。
 *
 * <p>这层只负责两类最基础的数据操作：
 * 1. 把业务事务里生成的 outbox 事件写入数据库
 * 2. 让扫描投递器按“可发送条件”捞出一小批待投递事件</p>
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventEntity> {

	default OutboxEventEntity save(OutboxEventEntity entity) {
		// 和项目里其他 mapper 保持同一套 save 语义：
		// id 为空时插入，否则按主键更新。
		if (entity.getId() == null) {
			insert(entity);
		} else {
			updateById(entity);
		}

		return entity;
	}

	default List<OutboxEventEntity> findDispatchableBatch(Instant now, int limit) {
		// 扫描器只捞“现在可以投递”的事件：
		// - PENDING：首次待发送
		// - FAILED 且 nextRetryAt <= now：到了下一次允许重试的时间
		// 再按 createdAt 升序取一小批，尽量先发更早进入 outbox 的事件。
		return selectList(
			Wrappers.<OutboxEventEntity>lambdaQuery()
				.and(wrapper -> wrapper
					.eq(OutboxEventEntity::getStatus, OutboxEventStatus.PENDING)
					.or(orWrapper -> orWrapper
						.eq(OutboxEventEntity::getStatus, OutboxEventStatus.FAILED)
						.le(OutboxEventEntity::getNextRetryAt, now)
					)
				)
				.orderByAsc(OutboxEventEntity::getCreatedAt)
				.last("limit " + limit)
		);
	}

	default Optional<OutboxEventEntity> findById(Long id) {
		return Optional.ofNullable(selectById(id));
	}

	/**
	 * 只回写发送结果相关字段，避免 updateById 时把 jsonb payload 按普通字符串更新。
	 */
	@Update("""
		update outbox_events
		set status = #{status},
			retry_count = #{retryCount},
			next_retry_at = #{nextRetryAt},
			last_error = #{lastError},
			sent_at = #{sentAt},
			updated_at = current_timestamp
		where id = #{id}
		""")
	int updateDispatchResult(
		@Param("id") Long id,
		@Param("status") OutboxEventStatus status,
		@Param("retryCount") Integer retryCount,
		@Param("nextRetryAt") Instant nextRetryAt,
		@Param("lastError") String lastError,
		@Param("sentAt") Instant sentAt
	);
}
