package com.example.msa.product.application.exception;

import java.util.UUID;

public class ProductNotfoundException extends RuntimeException{
    public ProductNotfoundException(UUID productId){
        super("Product not found. productId = " + productId);
    }
}
