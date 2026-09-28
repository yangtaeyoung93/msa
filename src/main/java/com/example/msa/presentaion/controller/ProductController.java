package com.example.msa.presentaion.controller;

import com.example.msa.presentaion.dto.request.ProductCreateRequest;
import com.example.msa.presentaion.dto.request.ProductUpdateRequest;
import com.example.msa.presentaion.dto.response.ProductResponse;
import com.example.msa.product.application.command.usecase.ProductCommandUseCase;
import com.example.msa.product.application.query.usecase.ProductQueryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCommandUseCase productCommandUseCase;
    private final ProductQueryUseCase productQueryUseCase;


    public ProductController(ProductCommandUseCase productCommandUseCase, ProductQueryUseCase productQueryUseCase) {
        this.productCommandUseCase = productCommandUseCase;
        this.productQueryUseCase = productQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(ProductResponse.from(productCommandUseCase.create(request)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID productId) {
        return ResponseEntity.ok(ProductResponse.from(productQueryUseCase.getById(productId)));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(productQueryUseCase.getAll().stream()
                .map(ProductResponse::from)
                .toList());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID productId,
                                  @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ProductResponse.from(productCommandUseCase.update(productId, request)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId) {
        productCommandUseCase.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
