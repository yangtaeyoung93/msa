package com.example.msa.product.application.command.usecase;

import com.example.msa.presentaion.dto.request.ProductCreateRequest;
import com.example.msa.presentaion.dto.request.ProductUpdateRequest;
import com.example.msa.product.domain.model.Product;

import java.util.UUID;

public interface ProductCommandUseCase {

    Product create(ProductCreateRequest request);

    Product update(UUID productId, ProductUpdateRequest request);

    void delete(UUID productId);
}
