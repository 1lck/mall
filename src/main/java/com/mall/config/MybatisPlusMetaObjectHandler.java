package com.mall.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 统一填充 MyBatis-Plus 审计字段，避免每个实体各写一套时间戳逻辑。
 */
@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

	/**
	 * 在插入时自动补齐常用审计字段。
	 */
	@Override
	public void insertFill(MetaObject metaObject) {
		Instant now = Instant.now();
		fillIfPresent(metaObject, "createdAt", now);
		fillIfPresent(metaObject, "updatedAt", now);
		fillIfPresent(metaObject, "processedAt", now);
	}

	/**
	 * 在更新时自动刷新更新时间。
	 */
	@Override
	public void updateFill(MetaObject metaObject) {
		fillIfPresent(metaObject, "updatedAt", Instant.now());
	}

	/**
	 * 仅在字段存在且当前值为空时写入默认值。
	 */
	private void fillIfPresent(MetaObject metaObject, String fieldName, Instant value) {
		if (!metaObject.hasSetter(fieldName) || getFieldValByName(fieldName, metaObject) != null) {
			return;
		}

		setFieldValByName(fieldName, value, metaObject);
	}
}
