package com.erp.backend.dto.user;

import org.springframework.data.domain.Page;

import java.util.List;

/** Dữ liệu phân trang gọn, ổn định cho Frontend (không trả thẳng Page của Spring). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
