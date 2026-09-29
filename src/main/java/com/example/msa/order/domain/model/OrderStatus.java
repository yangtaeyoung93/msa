package com.example.msa.order.domain.model;

public enum OrderStatus {
    CREATED,    // 결제 대기 (재고 선점 완료)
    PAID,       // 결제 완료
    CANCELED
}
