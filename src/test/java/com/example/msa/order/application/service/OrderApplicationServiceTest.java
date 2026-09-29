package com.example.msa.order.application.service;

import com.example.msa.order.application.event.OrderCanceledEvent;
import com.example.msa.order.application.event.OrderCreatedEvent;
import com.example.msa.order.application.exception.OrderAlreadyCanceledException;
import com.example.msa.order.application.exception.OrderNotFoundException;
import com.example.msa.order.domain.model.Order;
import com.example.msa.order.domain.model.OrderStatus;
import com.example.msa.order.domain.repository.OrderRepository;
import com.example.msa.order.presentaion.dto.request.OrderCancelRequest;
import com.example.msa.order.presentaion.dto.request.OrderCreateRequest;
import com.example.msa.order.presentaion.dto.request.OrderItemRequest;
import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    private Product product(UUID productId, String price) {
        return new Product(productId, UUID.randomUUID(), "상품", "설명", new BigDecimal(price), "ACTIVE");
    }

    // ===== create =====

    @Test
    @DisplayName("주문 생성: 가격 스냅샷과 총액을 계산해 저장하고 OrderCreatedEvent를 발행한다")
    void create_savesOrderAndPublishesEvent() {
        UUID buyerId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        when(productRepository.findById(p1)).thenReturn(Optional.of(product(p1, "1000")));
        when(productRepository.findById(p2)).thenReturn(Optional.of(product(p2, "250.50")));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderApplicationService.create(new OrderCreateRequest(buyerId.toString(), List.of(
                new OrderItemRequest(p1.toString(), 2),
                new OrderItemRequest(p2.toString(), 3))));

        // 1000*2 + 250.50*3 = 2751.50
        assertThat(result.getTotalPrice()).isEqualByComparingTo("2751.50");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getItems()).hasSize(2);

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(result.getId());
        assertThat(captor.getValue().actorId()).isEqualTo(buyerId);
        assertThat(captor.getValue().items()).hasSize(2);
    }

    @Test
    @DisplayName("주문 생성: buyerId가 UUID가 아니면 400")
    void create_invalidBuyerId_badRequest() {
        assertThatThrownBy(() -> orderApplicationService.create(new OrderCreateRequest("not-uuid",
                List.of(new OrderItemRequest(UUID.randomUUID().toString(), 1)))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("주문 생성: 항목이 비어있으면 400")
    void create_emptyItems_badRequest() {
        assertThatThrownBy(() -> orderApplicationService.create(
                new OrderCreateRequest(UUID.randomUUID().toString(), List.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("주문 생성: 수량이 1 미만이면 400")
    void create_quantityLessThanOne_badRequest() {
        UUID productId = UUID.randomUUID();

        assertThatThrownBy(() -> orderApplicationService.create(new OrderCreateRequest(
                UUID.randomUUID().toString(), List.of(new OrderItemRequest(productId.toString(), 0)))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("주문 생성: productId가 중복되면 400")
    void create_duplicateProductId_badRequest() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product(productId, "1000")));

        assertThatThrownBy(() -> orderApplicationService.create(new OrderCreateRequest(
                UUID.randomUUID().toString(), List.of(
                new OrderItemRequest(productId.toString(), 1),
                new OrderItemRequest(productId.toString(), 2)))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성: 상품이 없으면 ProductNotfoundException")
    void create_productNotFound_throws() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderApplicationService.create(new OrderCreateRequest(
                UUID.randomUUID().toString(), List.of(new OrderItemRequest(productId.toString(), 1)))))
                .isInstanceOf(ProductNotfoundException.class);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ===== getById =====

    @Test
    @DisplayName("주문 조회: 없으면 OrderNotFoundException")
    void getById_notFound_throws() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderApplicationService.getById(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // ===== cancel =====

    private Order existingOrder(UUID productId) {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID());
        order.addItem(productId, new BigDecimal("1000"), 2);
        return order;
    }

    @Test
    @DisplayName("주문 취소: 상태를 CANCELED로 바꾸고 OrderCanceledEvent를 발행한다")
    void cancel_changesStatusAndPublishesEvent() {
        UUID productId = UUID.randomUUID();
        Order order = existingOrder(productId);
        UUID modifierId = UUID.randomUUID();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Order result = orderApplicationService.cancel(order.getId(), new OrderCancelRequest(modifierId.toString()));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(result.getModifyId()).isEqualTo(modifierId);
        ArgumentCaptor<OrderCanceledEvent> captor = ArgumentCaptor.forClass(OrderCanceledEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().items()).hasSize(1);
        assertThat(captor.getValue().items().get(0).productId()).isEqualTo(productId);
        assertThat(captor.getValue().items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("주문 취소: 이미 취소된 주문이면 OrderAlreadyCanceledException, 이벤트는 발행하지 않는다")
    void cancel_alreadyCanceled_throws() {
        Order order = existingOrder(UUID.randomUUID());
        order.cancel(UUID.randomUUID());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderApplicationService.cancel(order.getId(),
                new OrderCancelRequest(UUID.randomUUID().toString())))
                .isInstanceOf(OrderAlreadyCanceledException.class);
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("주문 취소: 주문이 없으면 OrderNotFoundException")
    void cancel_notFound_throws() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderApplicationService.cancel(orderId,
                new OrderCancelRequest(UUID.randomUUID().toString())))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
