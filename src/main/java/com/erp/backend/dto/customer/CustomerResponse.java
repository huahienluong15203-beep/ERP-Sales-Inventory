package com.erp.backend.dto.customer;

import com.erp.backend.dto.user.RefItem;

import java.time.LocalDateTime;

/**
 * S3-03: Hồ sơ đại lý trả về cho Frontend.
 * region: {id, code, name} | salesRep: {id, code = tên tài khoản, name = họ tên} (null nếu chưa gán)
 */
public record CustomerResponse(
        Long id,
        String code,
        String name,
        String taxCode,
        String customerGroup,
        String customerGroupLabel,
        RefItem region,
        RefItem salesRep,
        String contactName,
        String phone,
        String email,
        String address,
        String note,
        String status,
        String statusReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
