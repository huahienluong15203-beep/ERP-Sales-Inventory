package com.erp.backend.controller;

import com.erp.backend.dto.discount.*;
import com.erp.backend.security.UserDetailsImpl;
import com.erp.backend.service.DiscountPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * S3-01: Chính sách chiết khấu theo sản lượng.
 * - Xem, tính chiết khấu: Admin, QL kinh doanh, NV kinh doanh, Kế toán.
 * - Tạo / sửa / ngừng áp dụng: Admin, QL kinh doanh. Không có xoá (chỉ ngừng áp dụng).
 */
@RestController
@RequestMapping("/api/discount-policies")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'SALES_REP', 'ACCOUNTANT')")
public class DiscountPolicyController {

    private final DiscountPolicyService discountPolicyService;

    /** Vd: ?status=ACTIVE&keyword=coca */
    @GetMapping
    public List<DiscountPolicyResponse> search(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword) {
        return discountPolicyService.search(status, keyword);
    }

    @GetMapping("/{id}")
    public DiscountPolicyResponse getById(@PathVariable Long id) {
        return discountPolicyService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<DiscountPolicyResponse> create(@RequestBody DiscountPolicyRequest request,
                                                         @AuthenticationPrincipal UserDetailsImpl actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discountPolicyService.create(request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public DiscountPolicyResponse update(@PathVariable Long id, @RequestBody DiscountPolicyRequest request,
                                         @AuthenticationPrincipal UserDetailsImpl actor) {
        return discountPolicyService.update(id, request, actor);
    }

    /** Vd: PATCH /api/discount-policies/3/status?status=INACTIVE */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public DiscountPolicyResponse changeStatus(@PathVariable Long id, @RequestParam String status,
                                               @AuthenticationPrincipal UserDetailsImpl actor) {
        return discountPolicyService.changeStatus(id, status, actor);
    }

    /** Tính chiết khấu cho 1 dòng hàng. Body: {"productSku":"SP-COCA","quantity":120,"unitPrice":10000} */
    @PostMapping("/calculate")
    public DiscountCalculationResponse calculate(@RequestBody DiscountCalculationRequest request) {
        return discountPolicyService.calculate(request);
    }
}
