package com.erp.backend.controller;

import com.erp.backend.dto.product.ProductImportPreviewResponse;
import com.erp.backend.dto.product.ProductImportSummaryResponse;
import com.erp.backend.service.ProductExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller quản lý danh mục sản phẩm và nhập hàng loạt từ Excel (S2-08).
 * Phân quyền: Quản trị hệ thống (ROLE_ADMIN) và Quản lý kinh doanh (ROLE_SALES_MANAGER).
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "API Quản lý sản phẩm & Nhập danh mục hàng loạt từ Excel (S2-08)")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
public class ProductManagementController {

    private final ProductExcelImportService productExcelImportService;

    /**
     * S2-08 AC1: Tải tệp mẫu Excel nhập danh mục sản phẩm (5.000 SKU).
     */
    @GetMapping("/import/template")
    @Operation(summary = "Tải tệp mẫu Excel sản phẩm", description = "Tải về tệp Excel mẫu chứa định dạng cột chuẩn và bảng hướng dẫn quy tắc nhập liệu.")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] excelBytes = productExcelImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Mau_Nhap_SanPham_ERP.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    /**
     * S2-08 AC1 & AC2: Xem trước dữ liệu và báo lỗi theo từng dòng trước khi nhập.
     * Đánh dấu rõ ràng dòng nào là CREATE (tạo mới) và dòng nào là UPDATE (cập nhật).
     */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Xem trước dữ liệu Excel sản phẩm", description = "Đọc tệp Excel, kiểm tra tính hợp lệ từng dòng, đánh dấu CREATE/UPDATE cho từng SKU và thống kê lỗi.")
    public ResponseEntity<ProductImportPreviewResponse> previewImport(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productExcelImportService.previewImport(file));
    }

    /**
     * S2-08: Thực thi nhập danh mục sản phẩm hàng loạt (hỗ trợ tới 5.000 SKU).
     * Dòng lỗi bị bỏ qua, dòng hợp lệ được tạo mới hoặc cập nhật, trả về báo cáo chi tiết.
     */
    @PostMapping(value = "/import/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Thực thi nhập danh mục sản phẩm", description = "Nhập danh mục sản phẩm hàng loạt từ Excel. Dòng lỗi bỏ qua, SKU cũ cập nhật, SKU mới tạo mới.")
    public ResponseEntity<ProductImportSummaryResponse> executeImport(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productExcelImportService.executeImport(file));
    }
}
