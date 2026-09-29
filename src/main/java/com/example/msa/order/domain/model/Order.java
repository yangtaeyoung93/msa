package com.example.msa.order.domain.model;

import com.example.msa.order.application.exception.OrderAlreadyCanceledException;
import com.example.msa.order.application.exception.OrderNotCancelableException;
import com.example.msa.order.application.exception.OrderNotPayableException;
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

    // 결제 대기(CREATED) 상태의 주문만 결제 완료로 바꿀 수 있다.
    public void markPaid(UUID actorId) {
        if (this.status != OrderStatus.CREATED) {
            throw new OrderNotPayableException(this.id, this.status);
        }
        this.status = OrderStatus.PAID;
        this.modifyId = actorId;
        this.modifyDt = LocalDateTime.now();
    }

    public void cancel(UUID actorId) {
        if (this.status == OrderStatus.CANCELED) {
            throw new OrderAlreadyCanceledException(this.id);
        }
        // 환불은 범위 밖이므로 결제 완료된 주문은 취소할 수 없다.
        if (this.status == OrderStatus.PAID) {
            throw new OrderNotCancelableException(this.id);
        }
        this.status = OrderStatus.CANCELED;
        this.modifyId = actorId;
        this.modifyDt = LocalDateTime.now();
    }
}
