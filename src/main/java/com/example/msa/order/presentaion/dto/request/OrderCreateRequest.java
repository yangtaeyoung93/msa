package com.example.msa.order.presentaion.dto.request;

import java.util.List;

public record OrderCreateRequest(
        String buyerId,
        List<OrderItemRequest> items
) {
}
