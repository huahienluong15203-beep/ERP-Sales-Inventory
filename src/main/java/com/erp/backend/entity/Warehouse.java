package com.erp.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Kho hàng — bản TỐI GIẢN phục vụ S1-09 (gắn thủ kho với kho).
 * Module kho đầy đủ (vị trí kệ, người phụ trách...) sẽ mở rộng ở Sprint 5 (S5-03).
 */
@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    // ACTIVE | INACTIVE
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
