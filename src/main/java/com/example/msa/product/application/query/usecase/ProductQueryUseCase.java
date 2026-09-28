package com.example.msa.product.application.query.usecase;

import com.example.msa.product.domain.model.Product;

import java.util.List;
import java.util.UUID;

public interface ProductQueryUseCase {

    Product getById(UUID productId);

    List<Product> getAll();
}
