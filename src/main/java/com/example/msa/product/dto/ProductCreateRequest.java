package com.example.msa.product.dto;

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
