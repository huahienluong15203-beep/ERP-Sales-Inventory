package com.erp.backend.repository;

import com.erp.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findBySkuIn(Collection<String> skus);

    /**
     * Truy vấn nhanh tập hợp SKU đã tồn tại trong DB, tránh N+1 khi import tới 5.000 sản phẩm.
     */
    @Query("SELECT p.sku FROM Product p WHERE p.sku IN :skus")
    Set<String> findExistingSkus(@Param("skus") Collection<String> skus);

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku, Pageable pageable);
}
