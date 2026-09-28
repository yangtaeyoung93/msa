package com.example.msa.product.application.query.service;

import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.application.query.usecase.ProductQueryUseCase;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.query.ProductQueryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductQueryService implements ProductQueryUseCase {

    private final ProductQueryRepository productQueryRepository;

    public ProductQueryService(ProductQueryRepository productQueryRepository) {
        this.productQueryRepository = productQueryRepository;
    }

    public Product getById(UUID productId) {
        return productQueryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotfoundException(productId));
    }

    @Override
    public List<Product> getAll() {
        return productQueryRepository.findAll();
    }
}
