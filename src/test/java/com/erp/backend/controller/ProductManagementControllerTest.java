package com.erp.backend.controller;

import com.erp.backend.dto.product.ProductImportPreviewResponse;
import com.erp.backend.dto.product.ProductImportSummaryResponse;
import com.erp.backend.service.ProductExcelImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit test ProductManagementController - S2-08 Nhập danh mục sản phẩm từ Excel")
class ProductManagementControllerTest {

    @Mock
    private ProductExcelImportService productExcelImportService;

    @InjectMocks
    private ProductManagementController productManagementController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productManagementController).build();
    }

    @Test
    @DisplayName("S2-08 AC1: Tải tệp mẫu Excel sản phẩm -> 200 OK kèm header attachment")
    void downloadTemplate_Success() throws Exception {
        byte[] fakeBytes = new byte[]{1, 2, 3, 4};
        when(productExcelImportService.generateTemplate()).thenReturn(fakeBytes);

        mockMvc.perform(get("/api/products/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=Mau_Nhap_SanPham_ERP.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(fakeBytes));
    }

    @Test
    @DisplayName("S2-08 AC1: Xem trước dữ liệu tệp Excel sản phẩm -> 200 OK")
    void previewImport_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "san_pham.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

        ProductImportPreviewResponse response = ProductImportPreviewResponse.builder()
                .totalRows(2)
                .validRows(2)
                .invalidRows(0)
                .createCount(1)
                .updateCount(1)
                .rows(List.of())
                .build();

        when(productExcelImportService.previewImport(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/products/import/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.createCount").value(1))
                .andExpect(jsonPath("$.updateCount").value(1));
    }

    @Test
    @DisplayName("S2-08: Thực thi nhập danh mục sản phẩm từ tệp Excel -> 200 OK")
    void executeImport_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "san_pham.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

        ProductImportSummaryResponse summary = ProductImportSummaryResponse.builder()
                .totalRows(2)
                .successCount(2)
                .createdCount(1)
                .updatedCount(1)
                .errorCount(0)
                .importedAt(LocalDateTime.now())
                .message("Nhập thành công")
                .build();

        when(productExcelImportService.executeImport(any())).thenReturn(summary);

        mockMvc.perform(multipart("/api/products/import/execute").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(0));
    }
}
