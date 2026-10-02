package com.example.msa.order.application.service;

import com.example.msa.order.application.event.OrderCanceledEvent;
import com.example.msa.order.application.event.OrderCreatedEvent;
import com.example.msa.order.application.event.OrderEventItem;
import com.example.msa.order.application.event.OrderPaidEvent;
import com.example.msa.order.application.exception.OrderNotFoundException;
import com.example.msa.order.application.exception.OrderNotPayableException;
import com.example.msa.order.application.usecase.OrderUseCase;
import com.example.msa.order.domain.model.Order;
import com.example.msa.order.domain.model.OrderStatus;
import com.example.msa.order.domain.repository.OrderRepository;
import com.example.msa.order.presentaion.dto.request.OrderCancelRequest;
import com.example.msa.order.presentaion.dto.request.OrderCreateRequest;
import com.example.msa.order.presentaion.dto.request.OrderItemRequest;
import com.example.msa.order.presentaion.dto.request.OrderPayRequest;
import com.example.msa.payment.application.exception.PaymentAmountMismatchException;
import com.example.msa.payment.application.exception.PaymentFailedException;
import com.example.msa.payment.application.exception.PaymentPendingException;
import com.example.msa.payment.domain.gateway.PaymentApproveCommand;
import com.example.msa.payment.domain.gateway.PaymentCancelCommand;
import com.example.msa.payment.domain.gateway.PaymentGateway;
import com.example.msa.payment.domain.model.Payment;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;
import com.example.msa.payment.domain.model.PaymentStatus;
import com.example.msa.payment.domain.repository.PaymentRepository;
import com.example.msa.payment.infrastructure.adapter.PaymentAdapterRegistry;
import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderApplicationService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PaymentAdapterRegistry paymentGatewayRegistry;
    private final PaymentRepository paymentRepository;
    private final TransactionTemplate transactionTemplate;

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

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order pay(UUID orderId, OrderPayRequest request) {
        UUID actorId = toUuid(request.modifierId(), "modifierId");
        PaymentProvider provider = PaymentProvider.from(request.provider());
        if (request.paymentKey() == null || request.paymentKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentKey must not be blank");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than 0");
        }
        PaymentGateway gateway = paymentGatewayRegistry.get(provider);

        Order order = findByIdOrThrow(orderId);
        if (request.amount().compareTo(order.getTotalPrice()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must equal order total price");
        }
        Optional<Payment> existing = paymentRepository.findByPaymentKey(request.paymentKey());
        if (existing.isPresent() && !existing.get().getOrderId().equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "paymentKey is already used by another order");
        }
        // 같은 paymentKey로 이미 승인된 주문에 다시 요청이 오면 현재 주문을 그대로 돌려준다(멱등).
        if (order.getStatus() == OrderStatus.PAID
                && existing.isPresent() && existing.get().getStatus() == PaymentStatus.APPROVED) {
            return order;
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new OrderNotPayableException(orderId, order.getStatus());
        }

        PaymentResult result = gateway.approve(
                new PaymentApproveCommand(request.paymentKey(), orderId, request.amount()));

        PayOutcome outcome = transactionTemplate.execute(status ->
                applyPaymentResult(orderId, actorId, gateway, result));

        if (outcome.failure() != null) {
            throw outcome.failure();
        }
        return outcome.order();
    }

    private PayOutcome applyPaymentResult(UUID orderId, UUID actorId, PaymentGateway gateway, PaymentResult result) {
        Order order = findByIdOrThrow(orderId);
        return switch (result.status()) {
            case APPROVED -> applyApproved(order, actorId, gateway, result);
            case FAILED, CANCELED -> applyNotApproved(order, actorId, result);
            case PENDING -> applyPending(order, actorId, result);
        };
    }

    private PayOutcome applyApproved(Order order, UUID actorId, PaymentGateway gateway, PaymentResult result) {
        // 승인 이후 주문 상태가 바뀌었거나 승인 금액이 주문 금액과 다르면 승인된 결제를 취소한다.
        if (order.getStatus() != OrderStatus.CREATED) {
            return compensate(order, actorId, gateway, result, "주문 상태 변경으로 결제 불가",
                    new OrderNotPayableException(order.getId(), order.getStatus()));
        }
        if (result.amount() == null || result.amount().compareTo(order.getTotalPrice()) != 0) {
            return compensate(order, actorId, gateway, result, "결제 금액 불일치",
                    new PaymentAmountMismatchException(order.getId(), order.getTotalPrice(), result.amount()));
        }
        savePayment(result, actorId);
        order.markPaid(actorId);
        applicationEventPublisher.publishEvent(new OrderPaidEvent(order.getId(), actorId));
        return new PayOutcome(order, null);
    }

    private PayOutcome applyNotApproved(Order order, UUID actorId, PaymentResult result) {
        savePayment(result, actorId);
        // 결제 대기 중인 주문만 취소하며, 취소 이벤트로 재고가 복원된다.
        if (order.getStatus() == OrderStatus.CREATED) {
            order.cancel(actorId);
            applicationEventPublisher.publishEvent(
                    new OrderCanceledEvent(order.getId(), actorId, toEventItems(order)));
        }
        return new PayOutcome(order, new PaymentFailedException(result.failureReason(), result.failureMessage()));
    }

    private PayOutcome applyPending(Order order, UUID actorId, PaymentResult result) {
        // 결제 결과를 확인 중이므로 이력만 남기고 주문 상태는 그대로 둔다.
        savePayment(result, actorId);
        return new PayOutcome(order, new PaymentPendingException(order.getId()));
    }

    private PayOutcome compensate(Order order, UUID actorId, PaymentGateway gateway, PaymentResult result,
                                  String reason, RuntimeException failure) {
        PaymentResult canceled = gateway.cancel(
                new PaymentCancelCommand(result.paymentKey(), order.getId(), result.amount(), reason));
        savePayment(canceled, actorId);
        return new PayOutcome(order, failure);
    }

    // 같은 paymentKey로 재시도된 결제는 기존 행에 최신 결과를 반영한다.
    private void savePayment(PaymentResult result, UUID actorId) {
        Payment payment = paymentRepository.findByPaymentKey(result.paymentKey())
                .map(existing -> {
                    existing.applyResult(result, actorId);
                    return existing;
                })
                .orElseGet(() -> Payment.from(result, actorId));
        paymentRepository.save(payment);
    }

    private record PayOutcome(Order order, RuntimeException failure) {
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
