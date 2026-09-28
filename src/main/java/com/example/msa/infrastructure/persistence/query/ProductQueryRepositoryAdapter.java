package com.example.msa.infrastructure.persistence.query;

import com.example.msa.infrastructure.persistence.ProductJpaRepository;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.query.ProductQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryAdapter implements ProductQueryRepository {

    private ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(UUID productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll();
    }
}
