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

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.unitConversions WHERE p.id = :id")
    Optional<Product> findByIdWithConversions(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.unitConversions WHERE LOWER(p.sku) = LOWER(:sku)")
    Optional<Product> findBySkuWithConversions(@Param("sku") String sku);

    @Query("SELECT p FROM Product p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR :category = '' OR p.category = :category) " +
            "AND (:status IS NULL OR :status = '' OR p.status = :status)")
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                 @Param("category") String category,
                                 @Param("status") String status,
                                 Pageable pageable);
}
