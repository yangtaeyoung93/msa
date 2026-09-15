package com.example.msa.product.service;

import com.example.msa.product.domain.Product;
import com.example.msa.product.dto.ProductCreateRequest;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    Product create(ProductCreateRequest request);

    Product getById(UUID productId);

    List<Product> getAll();

    Product update(UUID productId, ProductCreateRequest request);

    void delete(UUID productId);
}
