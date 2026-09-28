package com.example.msa.product.application.query.service;

import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.query.ProductQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductQueryRepository productQueryRepository;

    @InjectMocks
    private ProductQueryService productQueryService;

    @Test
    @DisplayName("getById: 존재하는 상품이면 조회 결과를 반환한다")
    void getById_withExistingProduct_returnsProduct() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(
                UUID.randomUUID(), "상품명", "설명",
                BigDecimal.valueOf(1000), 10, "ACTIVE", UUID.randomUUID()
        );
        when(productQueryRepository.findById(productId)).thenReturn(Optional.of(product));

        Product result = productQueryService.getById(productId);

        assertThat(result).isEqualTo(product);
    }

    @Test
    @DisplayName("getById: 존재하지 않는 productId면 ProductNotfoundException이 발생한다")
    void getById_withNonExistingProduct_throwsNotFound() {
        UUID productId = UUID.randomUUID();
        when(productQueryRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productQueryService.getById(productId))
                .isInstanceOf(ProductNotfoundException.class);
    }

    @Test
    @DisplayName("getAll: 상품이 여러 건이면 전체 목록을 반환한다")
    void getAll_withMultipleProducts_returnsList() {
        Product product1 = Product.create(
                UUID.randomUUID(), "상품1", "설명1",
                BigDecimal.valueOf(1000), 10, "ACTIVE", UUID.randomUUID()
        );
        Product product2 = Product.create(
                UUID.randomUUID(), "상품2", "설명2",
                BigDecimal.valueOf(2000), 20, "ACTIVE", UUID.randomUUID()
        );
        when(productQueryRepository.findAll()).thenReturn(List.of(product1, product2));

        List<Product> result = productQueryService.getAll();

        assertThat(result).hasSize(2).containsExactly(product1, product2);
    }

    @Test
    @DisplayName("getAll: 상품이 없으면 빈 목록을 반환한다")
    void getAll_withNoProducts_returnsEmptyList() {
        when(productQueryRepository.findAll()).thenReturn(List.of());

        List<Product> result = productQueryService.getAll();

        assertThat(result).isEmpty();
    }
}
