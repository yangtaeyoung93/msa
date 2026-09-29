package com.example.msa.order.application.service;

import com.example.msa.order.application.event.OrderCanceledEvent;
import com.example.msa.order.application.event.OrderCreatedEvent;
import com.example.msa.order.application.event.OrderEventItem;
import com.example.msa.order.application.exception.OrderNotFoundException;
import com.example.msa.order.application.usecase.OrderUseCase;
import com.example.msa.order.domain.model.Order;
import com.example.msa.order.domain.repository.OrderRepository;
import com.example.msa.order.presentaion.dto.request.OrderCancelRequest;
import com.example.msa.order.presentaion.dto.request.OrderCreateRequest;
import com.example.msa.order.presentaion.dto.request.OrderItemRequest;
import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderApplicationService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public Order create(OrderCreateRequest request) {
        UUID buyerId = toUuid(request.buyerId(), "buyerId");
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items must not be empty");
        }

        Order order = Order.create(buyerId, buyerId);
        Set<UUID> seenProductIds = new HashSet<>();
        for (OrderItemRequest item : request.items()) {
            UUID productId = toUuid(item.productId(), "productId");
            if (item.quantity() == null || item.quantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be at least 1");
            }
            if (!seenProductIds.add(productId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate productId: " + productId);
            }
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotfoundException(productId));
            order.addItem(productId, product.getPrice(), item.quantity());
        }

        Order saved = orderRepository.save(order);
        applicationEventPublisher.publishEvent(new OrderCreatedEvent(saved.getId(), buyerId, toEventItems(saved)));
        return saved;
    }

    @Override
    public Order getById(UUID orderId) {
        return findByIdOrThrow(orderId);
    }

    @Override
    @Transactional
    public Order cancel(UUID orderId, OrderCancelRequest request) {
        Order order = findByIdOrThrow(orderId);
        UUID actorId = toUuid(request.modifierId(), "modifierId");
        order.cancel(actorId);
        applicationEventPublisher.publishEvent(new OrderCanceledEvent(order.getId(), actorId, toEventItems(order)));
        return order;
    }

    private List<OrderEventItem> toEventItems(Order order) {
        return order.getItems().stream()
                .map(item -> new OrderEventItem(item.getProductId(), item.getQuantity()))
                .toList();
    }

    private UUID toUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be valid UUID");
        }
    }

    private Order findByIdOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
