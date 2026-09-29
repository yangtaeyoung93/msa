package com.example.msa.order.application.service;

import com.example.msa.order.application.event.OrderCanceledEvent;
import com.example.msa.order.application.event.OrderPaidEvent;
import com.example.msa.order.application.exception.OrderNotFoundException;
import com.example.msa.order.application.exception.OrderNotPayableException;
import com.example.msa.order.domain.model.Order;
import com.example.msa.order.domain.model.OrderStatus;
import com.example.msa.order.domain.repository.OrderRepository;
import com.example.msa.order.presentaion.dto.request.OrderPayRequest;
import com.example.msa.payment.application.exception.PaymentAmountMismatchException;
import com.example.msa.payment.application.exception.PaymentFailedException;
import com.example.msa.payment.application.exception.PaymentPendingException;
import com.example.msa.payment.application.exception.UnsupportedPaymentProviderException;
import com.example.msa.payment.domain.gateway.PaymentApproveCommand;
import com.example.msa.payment.domain.gateway.PaymentCancelCommand;
import com.example.msa.payment.domain.gateway.PaymentGateway;
import com.example.msa.payment.domain.model.Payment;
import com.example.msa.payment.domain.model.PaymentFailureReason;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.domain.model.PaymentResult;
import com.example.msa.payment.domain.model.PaymentStatus;
import com.example.msa.payment.domain.repository.PaymentRepository;
import com.example.msa.payment.infrastructure.gateway.PaymentGatewayRegistry;
import com.example.msa.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPayServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private PaymentGatewayRegistry paymentGatewayRegistry;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentGateway gateway;
    @Mock
    private PlatformTransactionManager transactionManager;

    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        service = new OrderApplicationService(orderRepository, productRepository, applicationEventPublisher,
                paymentGatewayRegistry, paymentRepository, new TransactionTemplate(transactionManager));
    }

    // 총액 2000 (1000 x 2)
    private Order order() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID());
        order.addItem(UUID.randomUUID(), new BigDecimal("1000"), 2);
        return order;
    }

    private OrderPayRequest request(String paymentKey, String amount) {
        return new OrderPayRequest(UUID.randomUUID().toString(), "TEST", paymentKey, new BigDecimal(amount));
    }

    private PaymentResult result(UUID orderId, String paymentKey, PaymentStatus status, String amount,
                                 PaymentFailureReason reason) {
        return new PaymentResult(PaymentProvider.TEST, paymentKey, orderId, new BigDecimal(amount), status, "CARD",
                status == PaymentStatus.APPROVED ? LocalDateTime.now() : null,
                reason, reason == null ? null : "결제 거절");
    }

    private void stubOrder(Order order) {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    }

    private void stubGateway() {
        when(paymentGatewayRegistry.get(PaymentProvider.TEST)).thenReturn(gateway);
    }

    // ===== 승인 =====

    @Test
    @DisplayName("결제: 승인되면 Payment(APPROVED)를 저장하고 주문을 PAID로 바꾸며 OrderPaidEvent를 발행한다")
    void pay_approved_marksOrderPaid() {
        Order order = order();
        stubOrder(order);
        stubGateway();
        when(gateway.approve(any(PaymentApproveCommand.class)))
                .thenReturn(result(order.getId(), "pay-1", PaymentStatus.APPROVED, "2000.00", null));

        Order result = service.pay(order.getId(), request("pay-1", "2000"));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(gateway).approve(new PaymentApproveCommand("pay-1", order.getId(), new BigDecimal("2000")));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(paymentCaptor.getValue().getOrderId()).isEqualTo(order.getId());
        assertThat(paymentCaptor.getValue().getPaymentKey()).isEqualTo("pay-1");

        ArgumentCaptor<OrderPaidEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(order.getId());
    }

    @Test
    @DisplayName("결제: 이전에 PENDING으로 저장된 같은 paymentKey는 새 행 대신 기존 행에 결과를 반영한다")
    void pay_retryWithSamePaymentKey_updatesExistingPayment() {
        Order order = order();
        Payment pending = Payment.from(
                result(order.getId(), "pay-1", PaymentStatus.PENDING, "2000.00", null), UUID.randomUUID());
        stubOrder(order);
        stubGateway();
        when(paymentRepository.findByPaymentKey("pay-1")).thenReturn(Optional.of(pending));
        when(gateway.approve(any(PaymentApproveCommand.class)))
                .thenReturn(result(order.getId(), "pay-1", PaymentStatus.APPROVED, "2000.00", null));

        service.pay(order.getId(), request("pay-1", "2000"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(pending);
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    // ===== 거절 / 보류 / 금액 불일치 =====

    @Test
    @DisplayName("결제: 거절되면 Payment(FAILED)를 저장하고 주문을 취소해 OrderCanceledEvent를 발행한 뒤 PaymentFailedException")
    void pay_rejected_cancelsOrderAndThrows() {
        Order order = order();
        stubOrder(order);
        stubGateway();
        when(gateway.approve(any(PaymentApproveCommand.class)))
                .thenReturn(result(order.getId(), "fail-1", PaymentStatus.FAILED, "2000.00",
                        PaymentFailureReason.CARD_DECLINED));

        assertThatThrownBy(() -> service.pay(order.getId(), request("fail-1", "2000")))
                .isInstanceOfSatisfying(PaymentFailedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(PaymentFailureReason.CARD_DECLINED));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(paymentCaptor.getValue().getFailureReason()).isEqualTo(PaymentFailureReason.CARD_DECLINED);

        ArgumentCaptor<OrderCanceledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCanceledEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().items()).hasSize(1);
        assertThat(eventCaptor.getValue().items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("결제: PG 승인 금액이 주문 금액과 다르면 PG 결제를 취소하고 주문은 유지한 채 PaymentAmountMismatchException")
    void pay_pgAmountMismatch_cancelsPaymentAndKeepsOrder() {
        Order order = order();
        stubOrder(order);
        stubGateway();
        when(gateway.approve(any(PaymentApproveCommand.class)))
                .thenReturn(result(order.getId(), "pay-1", PaymentStatus.APPROVED, "1000.00", null));
        when(gateway.cancel(any(PaymentCancelCommand.class)))
                .thenReturn(result(order.getId(), "pay-1", PaymentStatus.CANCELED, "1000.00", null));

        assertThatThrownBy(() -> service.pay(order.getId(), request("pay-1", "2000")))
                .isInstanceOf(PaymentAmountMismatchException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(gateway).cancel(any(PaymentCancelCommand.class));
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("결제: 결과가 PENDING이면 Payment(PENDING)만 저장하고 주문은 유지한 채 PaymentPendingException")
    void pay_pending_keepsOrderAndThrows() {
        Order order = order();
        stubOrder(order);
        stubGateway();
        when(gateway.approve(any(PaymentApproveCommand.class)))
                .thenReturn(result(order.getId(), "pay-1", PaymentStatus.PENDING, "2000.00", null));

        assertThatThrownBy(() -> service.pay(order.getId(), request("pay-1", "2000")))
                .isInstanceOf(PaymentPendingException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    // ===== 검증 =====

    @Test
    @DisplayName("결제: 요청 금액이 주문 총액과 다르면 400이고 PG를 호출하지 않는다")
    void pay_requestAmountMismatch_badRequest() {
        Order order = order();
        stubOrder(order);

        assertThatThrownBy(() -> service.pay(order.getId(), request("pay-1", "1500")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(gateway, never()).approve(any(PaymentApproveCommand.class));
    }

    @Test
    @DisplayName("결제: 취소된 주문은 OrderNotPayableException이고 PG를 호출하지 않는다")
    void pay_canceledOrder_throws() {
        Order order = order();
        order.cancel(UUID.randomUUID());
        stubOrder(order);

        assertThatThrownBy(() -> service.pay(order.getId(), request("pay-1", "2000")))
                .isInstanceOf(OrderNotPayableException.class);
        verify(gateway, never()).approve(any(PaymentApproveCommand.class));
    }

    @Test
    @DisplayName("결제: 같은 paymentKey로 이미 승인된 PAID 주문은 PG 호출 없이 현재 주문을 돌려준다(멱등)")
    void pay_alreadyPaidWithSamePaymentKey_isIdempotent() {
        Order order = order();
        order.markPaid(UUID.randomUUID());
        Payment approved = Payment.from(
                result(order.getId(), "pay-1", PaymentStatus.APPROVED, "2000.00", null), UUID.randomUUID());
        stubOrder(order);
        when(paymentRepository.findByPaymentKey("pay-1")).thenReturn(Optional.of(approved));

        Order result = service.pay(order.getId(), request("pay-1", "2000"));

        assertThat(result).isSameAs(order);
        verify(gateway, never()).approve(any(PaymentApproveCommand.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제: 다른 주문에서 이미 쓴 paymentKey면 409")
    void pay_paymentKeyUsedByOtherOrder_conflict() {
        Order order = order();
        Payment other = Payment.from(
                result(UUID.randomUUID(), "pay-1", PaymentStatus.APPROVED, "2000.00", null), UUID.randomUUID());
        stubOrder(order);
        when(paymentRepository.findByPaymentKey("pay-1")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.pay(order.getId(), request("pay-1", "2000")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(gateway, never()).approve(any(PaymentApproveCommand.class));
    }

    @Test
    @DisplayName("결제: 주문이 없으면 OrderNotFoundException")
    void pay_orderNotFound_throws() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pay(orderId, request("pay-1", "2000")))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("결제: 지원하지 않는 결제사면 UnsupportedPaymentProviderException")
    void pay_unsupportedProvider_throws() {
        OrderPayRequest request = new OrderPayRequest(
                UUID.randomUUID().toString(), "KAKAO", "pay-1", new BigDecimal("2000"));

        assertThatThrownBy(() -> service.pay(UUID.randomUUID(), request))
                .isInstanceOf(UnsupportedPaymentProviderException.class);
    }

    @Test
    @DisplayName("결제: paymentKey가 비어있으면 400")
    void pay_blankPaymentKey_badRequest() {
        assertThatThrownBy(() -> service.pay(UUID.randomUUID(), request(" ", "2000")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("결제: amount가 0 이하이면 400")
    void pay_nonPositiveAmount_badRequest() {
        assertThatThrownBy(() -> service.pay(UUID.randomUUID(), request("pay-1", "0")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("결제: modifierId가 UUID가 아니면 400")
    void pay_invalidModifierId_badRequest() {
        OrderPayRequest request = new OrderPayRequest("not-uuid", "TEST", "pay-1", new BigDecimal("2000"));

        assertThatThrownBy(() -> service.pay(UUID.randomUUID(), request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
