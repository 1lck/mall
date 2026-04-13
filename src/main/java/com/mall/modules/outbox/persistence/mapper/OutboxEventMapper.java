package com.mall.modules.outbox.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Outbox 事件仓储。
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventEntity> {

	default OutboxEventEntity save(OutboxEventEntity entity) {
		if (entity.getId() == null) {
			insert(entity);
		} else {
			updateById(entity);
		}

		return entity;
	}
}
