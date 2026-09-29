package com.example.msa.order.application.event;

import java.util.UUID;

public record OrderEventItem(UUID productId, int quantity) {
}
