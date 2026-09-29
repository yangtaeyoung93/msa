package com.example.msa.product.domain.model;

import com.example.msa.product.application.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 1단계: 옵션 없는 재고. PK는 productId와 동일하다. (옵션 도입 시 복합키/stockId로 마이그레이션 예정)
@Entity
@Getter
@Table(name = "\"stock\"", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {
    @Id
    private UUID id;


    @Column(nullable = false)
    private Integer stock;

    public Stock(UUID id, Integer stock) {
        this.id = id;
        this.stock = stock;
    }

    public static Stock create(UUID productId, Integer quantity) {
        return new Stock(productId, quantity);
    }

    public void changeStock(Integer quantity) {
        this.stock = quantity;
    }

    // 주문 시 재고 차감.
    public void decrease(int quantity) {
        if (quantity > this.stock) {
            throw new InsufficientStockException(this.id, quantity, this.stock);
        }
        this.stock -= quantity;
    }

    // 주문 취소 시 재고 복원.
    public void increase(int quantity) {
        this.stock += quantity;
    }
}
