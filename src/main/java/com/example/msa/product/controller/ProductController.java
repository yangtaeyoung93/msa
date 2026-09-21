package com.example.msa.product.controller;

import com.example.msa.product.domain.Product;
import com.example.msa.product.dto.ProductCreateRequest;
import com.example.msa.product.dto.ProductUpdateRequest;
import com.example.msa.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductCreateRequest request) {
        Product response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}")
    public Product getById(@PathVariable UUID productId) {
        return productService.getById(productId);
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.getAll();
    }

    @PutMapping("/{productId}")
    public Product update(@PathVariable UUID productId,
                          @RequestBody ProductUpdateRequest request) {
        return productService.update(productId, request);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId) {
        productService.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
