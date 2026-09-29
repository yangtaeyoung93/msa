package com.example.msa.product.domain.model;

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
}
