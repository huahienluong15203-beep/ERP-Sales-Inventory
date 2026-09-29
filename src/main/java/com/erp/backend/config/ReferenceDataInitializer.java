package com.erp.backend.config;

import com.erp.backend.entity.Region;
import com.erp.backend.entity.Warehouse;
import com.erp.backend.repository.RegionRepository;
import com.erp.backend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Dữ liệu mẫu kho và địa bàn để test S1-09 (chỉ tạo nếu chưa có).
 * Tách riêng khỏi DataInitializer để tránh conflict với phần việc của bạn khác.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceDataInitializer implements CommandLineRunner {

    private final WarehouseRepository warehouseRepository;
    private final RegionRepository regionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // Tự động kích hoạt extension unaccent trong PostgreSQL để hỗ trợ tìm kiếm tiếng Việt không dấu
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS unaccent;");
            log.info("PostgreSQL extension 'unaccent' đã được kích hoạt thành công.");
        } catch (Exception e) {
            log.warn("Không thể kích hoạt extension unaccent trên cơ sở dữ liệu: {}", e.getMessage());
        }

        seedWarehouse("KHO-HN", "Kho Tổng Hà Nội", "KCN Tiên Sơn, Bắc Ninh");
        seedWarehouse("KHO-HCM", "Kho Tổng TP.HCM", "KCN Tân Bình, TP.HCM");

        seedRegion("MB", "Miền Bắc");
        seedRegion("MT", "Miền Trung");
        seedRegion("MN", "Miền Nam");
    }

    private void seedWarehouse(String code, String name, String address) {
        if (!warehouseRepository.existsByCode(code)) {
            warehouseRepository.save(Warehouse.builder().code(code).name(name).address(address).build());
        }
    }

    private void seedRegion(String code, String name) {
        if (!regionRepository.existsByCode(code)) {
            regionRepository.save(Region.builder().code(code).name(name).build());
        }
    }
}
