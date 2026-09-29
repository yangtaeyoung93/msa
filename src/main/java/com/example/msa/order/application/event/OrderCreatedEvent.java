package com.example.msa.order.application.event;

import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, UUID actorId, List<OrderEventItem> items) {
}
