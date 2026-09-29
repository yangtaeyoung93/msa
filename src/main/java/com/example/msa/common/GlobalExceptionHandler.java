package com.example.msa.common;

import com.example.msa.order.application.exception.OrderAlreadyCanceledException;
import com.example.msa.order.application.exception.OrderNotFoundException;
import com.example.msa.product.application.exception.InsufficientStockException;
import com.example.msa.product.application.exception.ProductNotfoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotfoundException.class)
    public ResponseEntity<ErrorResponse> productNotfoundEx(ProductNotfoundException e, HttpServletRequest request){
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> orderNotFoundEx(OrderNotFoundException e, HttpServletRequest request){
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> insufficientStockEx(InsufficientStockException e, HttpServletRequest request){
        return build(HttpStatus.CONFLICT, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OrderAlreadyCanceledException.class)
    public ResponseEntity<ErrorResponse> orderAlreadyCanceledEx(OrderAlreadyCanceledException e, HttpServletRequest request){
        return build(HttpStatus.CONFLICT, e.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(response);
    }
}
