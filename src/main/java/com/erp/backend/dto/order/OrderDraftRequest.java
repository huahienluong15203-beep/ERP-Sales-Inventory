package com.erp.backend.dto.order;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * S3-09: Lưu nháp / xem trước đơn hàng.
 * deliveryAddressId để trống thì dùng điểm giao mặc định của đại lý.
 */
@Getter
@Setter
public class OrderDraftRequest {
    private Long customerId;
    private Long deliveryAddressId;
    private LocalDate desiredDeliveryDate;
    private String note;
    private List<OrderLineRequest> lines;
}
