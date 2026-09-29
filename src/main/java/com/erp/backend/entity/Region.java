package com.erp.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Địa bàn / khu vực bán hàng — phục vụ S1-09 (gắn nhân viên kinh doanh với địa bàn).
 * Dùng tiếp ở S3-06 (phân công đại lý theo khu vực).
 */
@Entity
@Table(name = "regions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    // ACTIVE | INACTIVE
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
