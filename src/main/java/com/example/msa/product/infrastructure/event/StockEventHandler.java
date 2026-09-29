package com.example.msa.product.infrastructure.event;

import com.example.msa.order.application.event.OrderCanceledEvent;
import com.example.msa.order.application.event.OrderCreatedEvent;
import com.example.msa.order.application.event.OrderEventItem;
import com.example.msa.product.application.event.ProductCreatedEvent;
import com.example.msa.product.application.exception.InsufficientStockException;
import com.example.msa.product.application.event.ProductDeletedEvent;
import com.example.msa.product.application.event.ProductUpdatedEvent;
import com.example.msa.product.domain.model.Stock;
import com.example.msa.product.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockEventHandler {

    private final StockRepository stockRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(ProductCreatedEvent event) {
        int quantity = event.stock() == null ? 0 : event.stock();
        stockRepository.save(Stock.create(event.productId(), quantity));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(ProductUpdatedEvent event) {
        // 재고 값이 없으면 기존 재고를 유지한다.
        if (event.stock() == null) {
            return;
        }
        Optional<Stock> existing = stockRepository.findById(event.productId());
        if (existing.isPresent()) {
            existing.get().changeStock(event.stock());
            stockRepository.save(existing.get());
        } else {
            stockRepository.save(Stock.create(event.productId(), event.stock()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(ProductDeletedEvent event) {
        if (stockRepository.findById(event.productId()).isPresent()) {
            stockRepository.deleteById(event.productId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(OrderCreatedEvent event) {
        for (OrderEventItem item : sortedByProductId(event.items())) {
            Stock stock = stockRepository.findByIdForUpdate(item.productId())
                    .orElseThrow(() -> new InsufficientStockException(item.productId(), item.quantity(), 0));
            stock.decrease(item.quantity());
            stockRepository.save(stock);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(OrderCanceledEvent event) {
        for (OrderEventItem item : sortedByProductId(event.items())) {
            stockRepository.findByIdForUpdate(item.productId()).ifPresent(stock -> {
                stock.increase(item.quantity());
                stockRepository.save(stock);
            });
        }
    }

    private List<OrderEventItem> sortedByProductId(List<OrderEventItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(OrderEventItem::productId))
                .toList();
    }
}
