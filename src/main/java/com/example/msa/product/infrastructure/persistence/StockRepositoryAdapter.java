package com.example.msa.product.infrastructure.persistence;

import com.example.msa.product.domain.model.Stock;
import com.example.msa.product.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockRepositoryAdapter implements StockRepository {

    private final StockJpaRepository stockJpaRepository;

    @Override
    public Stock save(Stock stock) {
        return stockJpaRepository.save(stock);
    }

    @Override
    public Optional<Stock> findById(UUID productId) {
        return stockJpaRepository.findById(productId);
    }

    @Override
    public Optional<Stock> findByIdForUpdate(UUID productId) {
        return stockJpaRepository.findByIdForUpdate(productId);
    }

    @Override
    public List<Stock> findAllById(Collection<UUID> productIds) {
        return stockJpaRepository.findAllById(productIds);
    }

    @Override
    public void deleteById(UUID productId) {
        stockJpaRepository.deleteById(productId);
    }
}
