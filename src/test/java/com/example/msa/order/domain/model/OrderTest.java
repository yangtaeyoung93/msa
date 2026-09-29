package com.example.msa.order.domain.model;

import com.example.msa.order.application.exception.OrderAlreadyCanceledException;
import com.example.msa.order.application.exception.OrderNotCancelableException;
import com.example.msa.order.application.exception.OrderNotPayableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order order() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID());
        order.addItem(UUID.randomUUID(), new BigDecimal("1000"), 2);
        return order;
    }

    @Test
    @DisplayName("결제 완료 처리: CREATED 주문은 PAID로 바뀌고 수정자가 기록된다")
    void markPaid_changesStatusToPaid() {
        Order order = order();
        UUID actorId = UUID.randomUUID();

        order.markPaid(actorId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getModifyId()).isEqualTo(actorId);
    }

    @Test
    @DisplayName("결제 완료 처리: 이미 PAID인 주문은 OrderNotPayableException")
    void markPaid_alreadyPaid_throws() {
        Order order = order();
        order.markPaid(UUID.randomUUID());

        assertThatThrownBy(() -> order.markPaid(UUID.randomUUID()))
                .isInstanceOf(OrderNotPayableException.class);
    }

    @Test
    @DisplayName("결제 완료 처리: 취소된 주문은 OrderNotPayableException")
    void markPaid_canceled_throws() {
        Order order = order();
        order.cancel(UUID.randomUUID());

        assertThatThrownBy(() -> order.markPaid(UUID.randomUUID()))
                .isInstanceOf(OrderNotPayableException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("주문 취소: PAID 주문은 OrderNotCancelableException이며 상태가 유지된다")
    void cancel_paid_throws() {
        Order order = order();
        order.markPaid(UUID.randomUUID());

        assertThatThrownBy(() -> order.cancel(UUID.randomUUID()))
                .isInstanceOf(OrderNotCancelableException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("주문 취소: CREATED 주문은 CANCELED로 바뀌고, 다시 취소하면 OrderAlreadyCanceledException")
    void cancel_created_thenAlreadyCanceled() {
        Order order = order();

        order.cancel(UUID.randomUUID());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThatThrownBy(() -> order.cancel(UUID.randomUUID()))
                .isInstanceOf(OrderAlreadyCanceledException.class);
    }
}
