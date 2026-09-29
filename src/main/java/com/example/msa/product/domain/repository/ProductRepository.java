package com.example.msa.product.domain.repository;


import com.example.msa.product.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(UUID productId);

    void delete(Product product);

    List<Product> findAll();
}
