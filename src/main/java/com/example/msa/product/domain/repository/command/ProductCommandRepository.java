package com.example.msa.product.domain.repository.command;


import com.example.msa.product.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductCommandRepository{
    Product save(Product product);

    Optional<Product> findById(UUID productId);

    void delete(Product product);
}
