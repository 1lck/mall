package com.mall.modules.outbox.application;

import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Outbox 后台查询应用服务。
 *
 * <p>这一层负责把后台查询条件翻译成仓储查询，
 * 再把数据库实体转换成管理端更容易直接展示的 VO。</p>
 */
@Service
public class OutboxAdminApplicationService {

	/** 后台观察页默认最多展示最近多少条 outbox 记录。 */
	private static final int DEFAULT_LIMIT = 200;
	/** 为了避免一次把过多历史记录拉给页面，限制单次查询上限。 */
	private static final int MAX_LIMIT = 500;

	private final OutboxEventMapper outboxEventMapper;

	public OutboxAdminApplicationService(OutboxEventMapper outboxEventMapper) {
		this.outboxEventMapper = outboxEventMapper;
	}

	/**
	 * 按后台筛选条件返回最近的 outbox 事件列表。
	 *
	 * @param status 可选状态筛选，为空时返回全部状态
	 * @param keyword 可选关键字，会匹配 eventId、aggregateId、eventType、topic、messageKey
	 * @param limit 返回条数上限，未传或非法时走默认值
	 * @return 管理端可直接展示的 outbox 列表
	 */
	public List<OutboxEventAdminVO> listEvents(OutboxEventStatus status, String keyword, Integer limit) {
		int safeLimit = normalizeLimit(limit);
		List<OutboxEventEntity> entities = outboxEventMapper.findAdminList(status, keyword, safeLimit);
		List<OutboxEventAdminVO> result = new ArrayList<>(entities.size());
		for (OutboxEventEntity entity : entities) {
			result.add(toAdminVO(entity));
		}
		return result;
	}

	/**
	 * 把 outbox 实体转换成后台观察页使用的 VO。
	 */
	private OutboxEventAdminVO toAdminVO(OutboxEventEntity entity) {
		return new OutboxEventAdminVO(
			entity.getId(),
			entity.getEventId(),
			entity.getAggregateType(),
			entity.getAggregateId(),
			entity.getEventType(),
			entity.getTopic(),
			entity.getMessageKey(),
			entity.getStatus(),
			entity.getRetryCount(),
			entity.getNextRetryAt(),
			entity.getLastError(),
			entity.getSentAt(),
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}

	/**
	 * 规范化前端传入的查询条数，避免查太多数据。
	 */
	private int normalizeLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
