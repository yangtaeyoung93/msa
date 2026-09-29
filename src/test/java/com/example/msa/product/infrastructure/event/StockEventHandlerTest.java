package com.example.msa.product.infrastructure.event;

import com.example.msa.product.application.event.ProductCreatedEvent;
import com.example.msa.product.application.event.ProductDeletedEvent;
import com.example.msa.product.application.event.ProductUpdatedEvent;
import com.example.msa.product.domain.model.Stock;
import com.example.msa.product.domain.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockEventHandlerTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockEventHandler stockEventHandler;

    // ===== ProductCreatedEvent =====

    @Test
    @DisplayName("생성 이벤트: 재고 값으로 Stock을 저장한다")
    void handleCreated_withStock_savesStock() {
        UUID productId = UUID.randomUUID();

        stockEventHandler.handle(new ProductCreatedEvent(productId, UUID.randomUUID(), 10));

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(productId);
        assertThat(captor.getValue().getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("생성 이벤트: 재고 값이 null이면 0으로 저장한다")
    void handleCreated_withNullStock_savesZero() {
        UUID productId = UUID.randomUUID();

        stockEventHandler.handle(new ProductCreatedEvent(productId, UUID.randomUUID(), null));

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().getStock()).isZero();
    }

    @Test
    @DisplayName("생성 이벤트: 저장 중 예외가 나면 그대로 전파한다 (상품 트랜잭션 롤백)")
    void handleCreated_whenSaveFails_propagates() {
        when(stockRepository.save(any(Stock.class))).thenThrow(new RuntimeException("DB 오류"));

        assertThatThrownBy(() ->
                stockEventHandler.handle(new ProductCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 오류");
    }

    // ===== ProductUpdatedEvent =====

    @Test
    @DisplayName("수정 이벤트: 기존 재고 행이 있으면 수량을 변경해 저장한다")
    void handleUpdated_withExistingStock_changesStock() {
        UUID productId = UUID.randomUUID();
        Stock existing = Stock.create(productId, 5);
        when(stockRepository.findById(productId)).thenReturn(Optional.of(existing));

        stockEventHandler.handle(new ProductUpdatedEvent(productId, UUID.randomUUID(), 20));

        assertThat(existing.getStock()).isEqualTo(20);
        verify(stockRepository).save(existing);
    }

    @Test
    @DisplayName("수정 이벤트: 재고 행이 없으면 새로 생성해 저장한다")
    void handleUpdated_withoutExistingStock_createsStock() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findById(productId)).thenReturn(Optional.empty());

        stockEventHandler.handle(new ProductUpdatedEvent(productId, UUID.randomUUID(), 20));

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(productId);
        assertThat(captor.getValue().getStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("수정 이벤트: 재고 값이 null이면 재고를 변경하지 않는다")
    void handleUpdated_withNullStock_doesNothing() {
        stockEventHandler.handle(new ProductUpdatedEvent(UUID.randomUUID(), UUID.randomUUID(), null));

        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("수정 이벤트: 처리 중 예외가 나면 그대로 전파한다 (상품 트랜잭션 롤백)")
    void handleUpdated_whenRepositoryFails_propagates() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findById(productId)).thenThrow(new RuntimeException("DB 오류"));

        assertThatThrownBy(() ->
                stockEventHandler.handle(new ProductUpdatedEvent(productId, UUID.randomUUID(), 20)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 오류");
    }

    // ===== ProductDeletedEvent =====

    @Test
    @DisplayName("삭제 이벤트: 재고 행이 있으면 삭제한다")
    void handleDeleted_withExistingStock_deletesStock() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findById(productId)).thenReturn(Optional.of(Stock.create(productId, 5)));

        stockEventHandler.handle(new ProductDeletedEvent(productId, UUID.randomUUID()));

        verify(stockRepository).deleteById(productId);
    }

    @Test
    @DisplayName("삭제 이벤트: 재고 행이 없으면 무시한다")
    void handleDeleted_withoutExistingStock_ignores() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findById(productId)).thenReturn(Optional.empty());

        stockEventHandler.handle(new ProductDeletedEvent(productId, UUID.randomUUID()));

        verify(stockRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("삭제 이벤트: 처리 중 예외가 나면 그대로 전파한다 (상품 트랜잭션 롤백)")
    void handleDeleted_whenRepositoryFails_propagates() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findById(productId)).thenThrow(new RuntimeException("DB 오류"));

        assertThatThrownBy(() ->
                stockEventHandler.handle(new ProductDeletedEvent(productId, UUID.randomUUID())))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 오류");
    }
}
