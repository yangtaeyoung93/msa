package com.example.msa.product.application.port.in;

import com.example.msa.product.domain.Product;
import com.example.msa.product.adapter.in.web.dto.ProductCreateRequest;
import com.example.msa.product.adapter.in.web.dto.ProductUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ProductUseCase {

    Product create(ProductCreateRequest request);

    Product getById(UUID productId);

    List<Product> getAll();

    Product update(UUID productId, ProductUpdateRequest request);

    void delete(UUID productId);
}
