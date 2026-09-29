package com.example.msa.order.infrastructure.event;

import com.example.msa.order.application.event.OrderCanceledEvent;
import com.example.msa.order.application.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OrderEventHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCreatedEvent event) {
        log.info("OrderCreatedEvent: orderId={}, actorId={}, itemCount={}",
                event.orderId(), event.actorId(), event.items().size());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCanceledEvent event) {
        log.info("OrderCanceledEvent: orderId={}, actorId={}, itemCount={}",
                event.orderId(), event.actorId(), event.items().size());
    }
}
