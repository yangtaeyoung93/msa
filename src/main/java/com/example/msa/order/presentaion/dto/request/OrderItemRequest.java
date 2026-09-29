package com.example.msa.order.presentaion.dto.request;

public record OrderItemRequest(
        String productId,
        Integer quantity
) {
}
