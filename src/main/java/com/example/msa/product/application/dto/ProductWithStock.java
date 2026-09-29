package com.example.msa.product.application.dto;

import com.example.msa.product.domain.model.Product;

public record ProductWithStock(Product product, Integer stock) {
}
