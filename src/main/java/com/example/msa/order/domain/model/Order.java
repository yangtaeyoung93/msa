package com.example.msa.order.domain.model;

import com.example.msa.order.application.exception.OrderAlreadyCanceledException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 테이블명 order는 SQL 예약어라 orders를 사용한다.
@Entity
@Getter
@Table(name = "\"orders\"", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "reg_id", nullable = false)
    private UUID regId;

    @Column(name = "reg_dt", nullable = false)
    private LocalDateTime regDt;

    @Column(name = "modify_id", nullable = false)
    private UUID modifyId;

    @Column(name = "modify_dt", nullable = false)
    private LocalDateTime modifyDt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(UUID buyerId, UUID actorId) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.buyerId = buyerId;
        order.status = OrderStatus.CREATED;
        order.totalPrice = BigDecimal.ZERO;
        order.regId = actorId;
        order.modifyId = actorId;
        order.regDt = LocalDateTime.now();
        order.modifyDt = order.regDt;
        return order;
    }

    // 항목 추가 시 총액(Σ 단가 × 수량)을 함께 누적한다.
    public void addItem(UUID productId, BigDecimal unitPrice, int quantity) {
        OrderItem item = new OrderItem(this, productId, unitPrice, quantity);
        this.items.add(item);
        this.totalPrice = this.totalPrice.add(item.subtotal());
    }

    public void cancel(UUID actorId) {
        if (this.status == OrderStatus.CANCELED) {
            throw new OrderAlreadyCanceledException(this.id);
        }
        this.status = OrderStatus.CANCELED;
        this.modifyId = actorId;
        this.modifyDt = LocalDateTime.now();
    }
}
