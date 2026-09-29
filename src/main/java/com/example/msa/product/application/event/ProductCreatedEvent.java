package com.example.msa.product.application.event;

import java.util.UUID;

public record ProductCreatedEvent(UUID productId, UUID actorId, Integer stock) {
}
