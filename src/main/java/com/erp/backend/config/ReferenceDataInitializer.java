package com.erp.backend.config;

import com.erp.backend.entity.Region;
import com.erp.backend.entity.Warehouse;
import com.erp.backend.repository.RegionRepository;
import com.erp.backend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Dữ liệu mẫu kho và địa bàn để test S1-09 (chỉ tạo nếu chưa có).
 * Tách riêng khỏi DataInitializer để tránh conflict với phần việc của bạn khác.
 */
@Component
@RequiredArgsConstructor
public class ReferenceDataInitializer implements CommandLineRunner {

    private final WarehouseRepository warehouseRepository;
    private final RegionRepository regionRepository;

    @Override
    public void run(String... args) {
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
