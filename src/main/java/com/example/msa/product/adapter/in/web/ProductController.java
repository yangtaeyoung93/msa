package com.example.msa.product.adapter.in.web;

import com.example.msa.product.domain.Product;
import com.example.msa.product.adapter.in.web.dto.ProductCreateRequest;
import com.example.msa.product.adapter.in.web.dto.ProductUpdateRequest;
import com.example.msa.product.application.port.in.ProductUseCase;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Product> create(@RequestBody ProductCreateRequest request) {
        Product response = productUseCase.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}")
    public Product getById(@PathVariable UUID productId) {
        return productUseCase.getById(productId);
    }

    @GetMapping
    public List<Product> getAll() {
        return productUseCase.getAll();
    }

    @PutMapping("/{productId}")
    public Product update(@PathVariable UUID productId,
                          @RequestBody ProductUpdateRequest request) {
        return productUseCase.update(productId, request);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId) {
        productUseCase.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
