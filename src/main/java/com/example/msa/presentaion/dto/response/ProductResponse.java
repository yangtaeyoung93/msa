package com.example.msa.presentaion.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID sellerId,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String status,
        UUID regId,
        LocalDateTime regDt,
        UUID modifyId,
        LocalDateTime modifyDt
) {
}
