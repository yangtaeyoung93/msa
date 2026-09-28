package com.example.msa.product.domain.repository.query;


import com.example.msa.product.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductQueryRepository{

    Optional<Product> findById(UUID productId);

    List<Product> findAll();

}
