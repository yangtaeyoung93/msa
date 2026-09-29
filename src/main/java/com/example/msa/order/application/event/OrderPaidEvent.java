package com.example.msa.order.application.event;

import java.util.UUID;

public record OrderPaidEvent(UUID orderId, UUID actorId) {
}
