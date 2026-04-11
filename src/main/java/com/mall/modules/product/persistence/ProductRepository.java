package com.mall.modules.product.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 商品仓储接口。
 */
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}
