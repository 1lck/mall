package com.mall.modules.user.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.modules.user.persistence.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口，统一封装用户表的常用查询和保存逻辑。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

	/**
	 * 按主键是否为空决定插入还是更新。
	 */
	default UserEntity save(UserEntity entity) {
		// 项目里统一使用 save 语义，避免业务层分别关心 insert 和 updateById。
		if (entity.getId() == null) {
			insert(entity);
		} else {
			updateById(entity);
		}

		return entity;
	}

	/**
	 * 按主键查询单个用户。
	 */
	default Optional<UserEntity> findById(Long id) {
		return Optional.ofNullable(selectById(id));
	}

	/**
	 * 查询全部用户。
	 */
	default List<UserEntity> findAll() {
		return selectList(null);
	}

	/**
	 * 按主键倒序返回用户列表。
	 */
	default List<UserEntity> findAllByOrderByIdDesc() {
		return selectList(Wrappers.<UserEntity>lambdaQuery().orderByDesc(UserEntity::getId));
	}

	/**
	 * 判断指定用户名是否已经存在。
	 */
	default boolean existsByUsername(String username) {
		return selectCount(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, username)) > 0;
	}

	/**
	 * 按用户名查询用户。
	 */
	default Optional<UserEntity> findByUsername(String username) {
		return Optional.ofNullable(selectOne(
			Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, username)
		));
	}
}
