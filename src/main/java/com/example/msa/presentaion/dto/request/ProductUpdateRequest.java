package com.example.msa.presentaion.dto.request;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String status,
        String modifierId
) {
}
