package com.example.msa.product.application.usecase;

import com.example.msa.product.application.dto.ProductWithStock;
import com.example.msa.product.presentaion.dto.request.ProductCreateRequest;
import com.example.msa.product.presentaion.dto.request.ProductUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ProductUseCase {

    ProductWithStock create(ProductCreateRequest request);

    ProductWithStock update(UUID productId, ProductUpdateRequest request);

    void delete(UUID productId);

    ProductWithStock getById(UUID productId);

    List<ProductWithStock> getAll();
}
