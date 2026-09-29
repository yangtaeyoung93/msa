package com.example.msa.product.presentaion.dto.response;

import com.example.msa.product.application.dto.ProductWithStock;
import com.example.msa.product.domain.model.Product;

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

    public static ProductResponse from(ProductWithStock productWithStock) {
        Product product = productWithStock.product();
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                productWithStock.stock(),
                product.getStatus(),
                product.getRegId(),
                product.getRegDt(),
                product.getModifyId(),
                product.getModifyDt()
        );
    }
}
