package com.example.msa.order.application.usecase;

import com.example.msa.order.domain.model.Order;
import com.example.msa.order.presentaion.dto.request.OrderCancelRequest;
import com.example.msa.order.presentaion.dto.request.OrderCreateRequest;

import java.util.UUID;

public interface OrderUseCase {
    Order create(OrderCreateRequest request);

    Order getById(UUID orderId);

    Order cancel(UUID orderId, OrderCancelRequest request);
}
