package com.example.msa.order.presentaion.controller;

import com.example.msa.order.application.usecase.OrderUseCase;
import com.example.msa.order.presentaion.dto.request.OrderCancelRequest;
import com.example.msa.order.presentaion.dto.request.OrderCreateRequest;
import com.example.msa.order.presentaion.dto.response.OrderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderUseCase orderUseCase;

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(OrderResponse.from(orderUseCase.create(request)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(OrderResponse.from(orderUseCase.getById(orderId)));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID orderId,
                                                @RequestBody OrderCancelRequest request) {
        return ResponseEntity.ok(OrderResponse.from(orderUseCase.cancel(orderId, request)));
    }
}
