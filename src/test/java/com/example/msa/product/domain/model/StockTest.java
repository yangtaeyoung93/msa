package com.example.msa.product.domain.model;

import com.example.msa.product.application.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    @DisplayName("재고 차감: 요청 수량만큼 감소한다")
    void decrease_reducesStock() {
        Stock stock = Stock.create(UUID.randomUUID(), 10);

        stock.decrease(3);

        assertThat(stock.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("재고 차감: 재고 전량 차감은 허용한다 (경계값)")
    void decrease_exactQuantity_allowed() {
        Stock stock = Stock.create(UUID.randomUUID(), 5);

        stock.decrease(5);

        assertThat(stock.getStock()).isZero();
    }

    @Test
    @DisplayName("재고 차감: 재고보다 많이 요청하면 예외를 던지고 수량은 유지된다")
    void decrease_insufficient_throws() {
        Stock stock = Stock.create(UUID.randomUUID(), 2);

        assertThatThrownBy(() -> stock.decrease(3))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(stock.getStock()).isEqualTo(2);
    }

    @Test
    @DisplayName("재고 복원: 요청 수량만큼 증가한다")
    void increase_addsStock() {
        Stock stock = Stock.create(UUID.randomUUID(), 2);

        stock.increase(3);

        assertThat(stock.getStock()).isEqualTo(5);
    }
}
