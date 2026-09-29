package com.example.msa.product.domain.repository;

import com.example.msa.product.domain.model.Stock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository {
    Stock save(Stock stock);

    Optional<Stock> findById(UUID productId);

    List<Stock> findAllById(Collection<UUID> productIds);

    void deleteById(UUID productId);
}
