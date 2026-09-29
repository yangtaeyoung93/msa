package com.example.msa.product.application.service;

import com.example.msa.product.application.dto.ProductWithStock;
import com.example.msa.product.application.event.ProductCreatedEvent;
import com.example.msa.product.application.event.ProductDeletedEvent;
import com.example.msa.product.application.event.ProductUpdatedEvent;
import com.example.msa.product.application.usecase.ProductUseCase;
import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.model.Stock;
import com.example.msa.product.domain.repository.ProductRepository;
import com.example.msa.product.domain.repository.StockRepository;
import com.example.msa.product.presentaion.dto.request.ProductCreateRequest;
import com.example.msa.product.presentaion.dto.request.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductApplicationService implements ProductUseCase {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final ApplicationEventPublisher applicationEventPublisher;


    @Override
    @Transactional
    public ProductWithStock create(ProductCreateRequest request) {
        UUID actorId = toUuid(request.creatorId(), "creatorId");
        Product product = Product.create(
                toUuid(request.sellerId(), "sellerId"),
                request.name(),
                request.description(),
                request.price(),
                request.status(),
                actorId
        );
        Product saved = productRepository.save(product);
        applicationEventPublisher.publishEvent(new ProductCreatedEvent(saved.getId(), actorId, request.stock()));
        return new ProductWithStock(saved, request.stock());
    }

    @Override
    @Transactional
    public ProductWithStock update(UUID productId, ProductUpdateRequest request) {
        Product product = findByIdOrThrow(productId);
        UUID actorId = toUuid(request.modifierId(), "creatorId");
        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.status(),
                actorId
        );
        applicationEventPublisher.publishEvent(new ProductUpdatedEvent(product.getId(), actorId, request.stock()));
        Integer stock = request.stock() != null ? request.stock() : findStock(product.getId());
        return new ProductWithStock(product, stock);
    }

    @Override
    @Transactional
    public void delete(UUID productId) {
        Product product = findByIdOrThrow(productId);
        productRepository.delete(product);
        applicationEventPublisher.publishEvent(new ProductDeletedEvent(product.getId(), product.getModifyId()));
    }

    @Override
    public ProductWithStock getById(UUID productId) {
        Product product = findByIdOrThrow(productId);
        return new ProductWithStock(product, findStock(product.getId()));
    }

    @Override
    public List<ProductWithStock> getAll() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return List.of();
        }
        // N+1을 피하기 위해 재고를 한 번에 조회해 조합한다. 재고 행이 없으면 null.
        List<UUID> productIds = products.stream().map(Product::getId).toList();
        Map<UUID, Integer> stockByProductId = stockRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Stock::getId, Stock::getStock));
        return products.stream()
                .map(product -> new ProductWithStock(product, stockByProductId.get(product.getId())))
                .toList();
    }

    private Integer findStock(UUID productId) {
        return stockRepository.findById(productId).map(Stock::getStock).orElse(null);
    }

    private UUID toUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be valid UUID");
        }
    }

    private Product findByIdOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotfoundException(productId));
    }

}
