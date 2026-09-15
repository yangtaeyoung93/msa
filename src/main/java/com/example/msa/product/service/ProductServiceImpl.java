package com.example.msa.product.service;

import com.example.msa.product.domain.Product;
import com.example.msa.product.dto.ProductCreateRequest;
import com.example.msa.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService{

    private ProductRepository productRepository;

    @Override
    @Transactional
    public Product create(ProductCreateRequest request) {
        Product.create(
                toUuId
        )
        return null;
    }

    @Override
    public Product getById(UUID productId) {
        return null;
    }

    @Override
    public List<Product> getAll() {
        return null;
    }

    @Override
    @Transactional
    public Product update(UUID productId, ProductCreateRequest request) {
        return null;
    }

    @Override
    @Transactional
    public void delete(UUID productId) {

    }

    private UUID toUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be valid UUID");
        }
    }
}
