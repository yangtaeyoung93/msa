package com.example.msa.product.adapter.in.web.dto;

import java.math.BigDecimal;

public record ProductCreateRequest(
        String sellerId,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String status,
        String creatorId
) {
}
