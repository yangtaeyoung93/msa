package com.example.msa.infrastructure.persistence.command;

import com.example.msa.infrastructure.persistence.ProductJpaRepository;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.command.ProductCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductCommandRepositoryAdapter implements ProductCommandRepository {

    private ProductJpaRepository productJpaRepository;


    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(UUID productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public void delete(Product product) {
        productJpaRepository.delete(product);

    }
}
