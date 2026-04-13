package com.mall.modules.product.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.modules.product.persistence.entity.ProductEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 商品 Mapper。
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductEntity> {

	default ProductEntity save(ProductEntity entity) {
		if (entity.getId() == null) {
			insert(entity);
		} else {
			updateById(entity);
		}

		return entity;
	}

	default Optional<ProductEntity> findById(Long id) {
		return Optional.ofNullable(selectById(id));
	}

	default List<ProductEntity> findAll() {
		return selectList(null);
	}

	default List<ProductEntity> findAllByOrderByIdDesc() {
		return selectList(Wrappers.<ProductEntity>lambdaQuery().orderByDesc(ProductEntity::getId));
	}

	default void delete(ProductEntity entity) {
		deleteById(entity.getId());
	}
}
