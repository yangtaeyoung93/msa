package com.example.msa.product.application.command.service;

import com.example.msa.presentaion.dto.request.ProductCreateRequest;
import com.example.msa.presentaion.dto.request.ProductUpdateRequest;
import com.example.msa.product.application.command.usecase.ProductCommandUseCase;
import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.command.ProductCommandRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductCommandService implements ProductCommandUseCase {

    private final ProductCommandRepository productCommandRepository;


    public ProductCommandService(ProductCommandRepository productCommandRepository) {
        this.productCommandRepository = productCommandRepository;
    }

    @Override
    @Transactional
    public Product create(ProductCreateRequest request) {
        Product product = Product.create(
                toUuid(request.sellerId(), "sellerId"),
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.status(),
                toUuid(request.creatorId(), "creatorId")
        );
        return productCommandRepository.save(product);
    }

    @Override
    @Transactional
    public Product update(UUID productId, ProductUpdateRequest request) {
        Product product = findByIdOrThrow(productId);
        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.status(),
                toUuid(request.modifierId(),"modifierId")
        );

        return product;
    }

    @Override
    @Transactional
    public void delete(UUID productId) {
        productCommandRepository.delete(findByIdOrThrow(productId));
    }

    private UUID toUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be valid UUID");
        }
    }

    private Product findByIdOrThrow(UUID productId) {
        return productCommandRepository.findById(productId)
                .orElseThrow(() -> new ProductNotfoundException(productId));
    }
}
