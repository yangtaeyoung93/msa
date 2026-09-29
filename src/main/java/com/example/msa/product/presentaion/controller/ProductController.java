package com.example.msa.product.presentaion.controller;

import com.example.msa.product.presentaion.dto.request.ProductCreateRequest;
import com.example.msa.product.presentaion.dto.request.ProductUpdateRequest;
import com.example.msa.product.presentaion.dto.response.ProductResponse;
import com.example.msa.product.application.usecase.ProductUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductUseCase productUseCase;

    public ProductController(ProductUseCase productUseCase) {
        this.productUseCase = productUseCase;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(ProductResponse.from(productUseCase.create(request)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID productId) {
        return ResponseEntity.ok(ProductResponse.from(productUseCase.getById(productId)));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(productUseCase.getAll().stream()
                .map(ProductResponse::from)
                .toList());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID productId,
                                  @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ProductResponse.from(productUseCase.update(productId, request)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId) {
        productUseCase.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
