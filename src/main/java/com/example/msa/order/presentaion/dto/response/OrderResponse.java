package com.example.msa.order.presentaion.dto.response;

import com.example.msa.order.domain.model.Order;
import com.example.msa.order.domain.model.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID buyerId,
        String status,
        BigDecimal totalPrice,
        List<Item> items,
        UUID regId,
        LocalDateTime regDt,
        UUID modifyId,
        LocalDateTime modifyDt
) {

    public record Item(UUID productId, BigDecimal unitPrice, int quantity) {
        static Item from(OrderItem item) {
            return new Item(item.getProductId(), item.getUnitPrice(), item.getQuantity());
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBuyerId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getItems().stream().map(Item::from).toList(),
                order.getRegId(),
                order.getRegDt(),
                order.getModifyId(),
                order.getModifyDt()
        );
    }
}
