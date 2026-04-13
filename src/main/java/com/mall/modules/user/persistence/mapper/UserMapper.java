package com.mall.modules.user.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.modules.user.persistence.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

	default UserEntity save(UserEntity entity) {
		if (entity.getId() == null) {
			insert(entity);
		} else {
			updateById(entity);
		}

		return entity;
	}

	default Optional<UserEntity> findById(Long id) {
		return Optional.ofNullable(selectById(id));
	}

	default List<UserEntity> findAll() {
		return selectList(null);
	}

	default List<UserEntity> findAllByOrderByIdDesc() {
		return selectList(Wrappers.<UserEntity>lambdaQuery().orderByDesc(UserEntity::getId));
	}

	default boolean existsByUsername(String username) {
		return selectCount(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, username)) > 0;
	}

	default Optional<UserEntity> findByUsername(String username) {
		return Optional.ofNullable(selectOne(
			Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, username)
		));
	}
}
