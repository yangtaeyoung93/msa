package com.example.msa.product.infrastructure.persistence;

import com.example.msa.product.domain.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockJpaRepository extends JpaRepository<Stock, UUID> {
}
