package com.example.msa.product.application.service;

import com.example.msa.product.application.dto.ProductWithStock;
import com.example.msa.product.application.event.ProductCreatedEvent;
import com.example.msa.product.application.event.ProductDeletedEvent;
import com.example.msa.product.application.event.ProductUpdatedEvent;
import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.model.Stock;
import com.example.msa.product.domain.repository.ProductRepository;
import com.example.msa.product.domain.repository.StockRepository;
import com.example.msa.product.presentaion.dto.request.ProductCreateRequest;
import com.example.msa.product.presentaion.dto.request.ProductUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ProductApplicationService productApplicationService;

    private Product existingProduct() {
        return Product.create(
                UUID.randomUUID(), "기존이름", "기존설명",
                BigDecimal.valueOf(500), "ACTIVE", UUID.randomUUID()
        );
    }

    private ProductUpdateRequest updateRequest(Integer stock, String modifierId) {
        return new ProductUpdateRequest(
                "새이름", "새설명", BigDecimal.valueOf(2000), stock, "INACTIVE", modifierId
        );
    }

    // ===== create =====

    @Test
    @DisplayName("create: 유효한 요청이면 상품을 저장하고, 요청한 재고를 담은 생성 이벤트를 발행한다")
    void create_withValidRequest_savesProductAndPublishesEventWithStock() {
        ProductCreateRequest request = new ProductCreateRequest(
                UUID.randomUUID().toString(),
                "상품명",
                "설명",
                BigDecimal.valueOf(1000),
                10,
                "ACTIVE",
                UUID.randomUUID().toString()
        );
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductWithStock result = productApplicationService.create(request);

        assertThat(result.product().getName()).isEqualTo("상품명");
        assertThat(result.product().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(result.stock()).isEqualTo(10);
        verify(productRepository, times(1)).save(any(Product.class));

        ArgumentCaptor<ProductCreatedEvent> captor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(result.product().getId());
        assertThat(captor.getValue().stock()).isEqualTo(10);
    }

    @Test
    @DisplayName("create: sellerId가 UUID 형식이 아니면 BAD_REQUEST 예외가 발생하고 저장/이벤트 발행은 일어나지 않는다")
    void create_withInvalidSellerId_throwsBadRequest() {
        ProductCreateRequest request = new ProductCreateRequest(
                "invalid-uuid",
                "상품명",
                "설명",
                BigDecimal.valueOf(1000),
                10,
                "ACTIVE",
                UUID.randomUUID().toString()
        );

        assertThatThrownBy(() -> productApplicationService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("sellerId");

        verify(productRepository, never()).save(any(Product.class));
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("create: creatorId가 UUID 형식이 아니면 BAD_REQUEST 예외가 발생하고 저장/이벤트 발행은 일어나지 않는다")
    void create_withInvalidCreatorId_throwsBadRequest() {
        ProductCreateRequest request = new ProductCreateRequest(
                UUID.randomUUID().toString(),
                "상품명",
                "설명",
                BigDecimal.valueOf(1000),
                10,
                "ACTIVE",
                "invalid-uuid"
        );

        assertThatThrownBy(() -> productApplicationService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("creatorId");

        verify(productRepository, never()).save(any(Product.class));
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    // ===== update =====

    @Test
    @DisplayName("update: 재고 값이 있으면 필드를 갱신하고, 재고를 담은 수정 이벤트를 발행하며 재고를 따로 조회하지 않는다")
    void update_withStock_updatesFieldsAndPublishesEventWithStock() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct()));

        ProductWithStock result = productApplicationService.update(
                productId, updateRequest(20, UUID.randomUUID().toString()));

        assertThat(result.product().getName()).isEqualTo("새이름");
        assertThat(result.product().getDescription()).isEqualTo("새설명");
        assertThat(result.product().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(result.product().getStatus()).isEqualTo("INACTIVE");
        assertThat(result.stock()).isEqualTo(20);

        ArgumentCaptor<ProductUpdatedEvent> captor = ArgumentCaptor.forClass(ProductUpdatedEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().stock()).isEqualTo(20);
        verify(stockRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("update: 재고 값이 null이면 현재 재고를 조회해 응답하고, 이벤트의 재고는 null이다")
    void update_withNullStock_returnsCurrentStock() {
        UUID productId = UUID.randomUUID();
        Product existing = existingProduct();
        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(stockRepository.findById(existing.getId()))
                .thenReturn(Optional.of(Stock.create(existing.getId(), 7)));

        ProductWithStock result = productApplicationService.update(
                productId, updateRequest(null, UUID.randomUUID().toString()));

        assertThat(result.stock()).isEqualTo(7);
        ArgumentCaptor<ProductUpdatedEvent> captor = ArgumentCaptor.forClass(ProductUpdatedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().stock()).isNull();
    }

    @Test
    @DisplayName("update: 재고 값이 null이고 재고 행도 없으면 stock은 null이다")
    void update_withNullStockAndNoStockRow_returnsNullStock() {
        UUID productId = UUID.randomUUID();
        Product existing = existingProduct();
        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(stockRepository.findById(existing.getId())).thenReturn(Optional.empty());

        ProductWithStock result = productApplicationService.update(
                productId, updateRequest(null, UUID.randomUUID().toString()));

        assertThat(result.stock()).isNull();
    }

    @Test
    @DisplayName("update: 존재하지 않는 productId면 ProductNotfoundException이 발생하고 이벤트는 발행되지 않는다")
    void update_withNonExistingProduct_throwsNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productApplicationService.update(productId, updateRequest(20, UUID.randomUUID().toString())))
                .isInstanceOf(ProductNotfoundException.class);

        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("update: modifierId가 UUID 형식이 아니면 BAD_REQUEST 예외가 발생하고 이벤트는 발행되지 않는다")
    void update_withInvalidModifierId_throwsBadRequest() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct()));

        // 주의: 현재 main 코드는 modifierId 검증 실패 메시지에 "creatorId"를 사용한다 (오타로 추정).
        assertThatThrownBy(() -> productApplicationService.update(productId, updateRequest(20, "invalid-uuid")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("creatorId");

        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    // ===== delete =====

    @Test
    @DisplayName("delete: 존재하는 상품이면 삭제하고 삭제 이벤트를 발행한다")
    void delete_withExistingProduct_deletesProductAndPublishesEvent() {
        UUID productId = UUID.randomUUID();
        Product existing = existingProduct();
        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        productApplicationService.delete(productId);

        verify(productRepository, times(1)).delete(existing);
        verify(applicationEventPublisher, times(1)).publishEvent(isA(ProductDeletedEvent.class));
    }

    @Test
    @DisplayName("delete: 존재하지 않는 productId면 ProductNotfoundException이 발생하고 delete/이벤트 발행은 일어나지 않는다")
    void delete_withNonExistingProduct_throwsNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productApplicationService.delete(productId))
                .isInstanceOf(ProductNotfoundException.class);

        verify(productRepository, never()).delete(any(Product.class));
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    // ===== getById =====

    @Test
    @DisplayName("getById: 존재하는 상품이면 상품과 재고를 함께 반환한다")
    void getById_withExistingProduct_returnsProductWithStock() {
        UUID productId = UUID.randomUUID();
        Product product = existingProduct();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockRepository.findById(product.getId()))
                .thenReturn(Optional.of(Stock.create(product.getId(), 5)));

        ProductWithStock result = productApplicationService.getById(productId);

        assertThat(result.product()).isSameAs(product);
        assertThat(result.stock()).isEqualTo(5);
    }

    @Test
    @DisplayName("getById: 재고 행이 아직 없으면 stock은 null이다")
    void getById_withoutStockRow_returnsNullStock() {
        UUID productId = UUID.randomUUID();
        Product product = existingProduct();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockRepository.findById(product.getId())).thenReturn(Optional.empty());

        ProductWithStock result = productApplicationService.getById(productId);

        assertThat(result.product()).isSameAs(product);
        assertThat(result.stock()).isNull();
    }

    @Test
    @DisplayName("getById: 존재하지 않는 productId면 ProductNotfoundException이 발생한다")
    void getById_withNonExistingProduct_throwsNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productApplicationService.getById(productId))
                .isInstanceOf(ProductNotfoundException.class);
    }

    // ===== getAll =====

    @Test
    @DisplayName("getAll: 상품이 여러 건이면 재고를 한 번에 조회해 조합하고, 재고 행이 없는 상품은 null이다")
    void getAll_withMultipleProducts_combinesStockWithSingleQuery() {
        Product product1 = existingProduct();
        Product product2 = existingProduct();
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));
        when(stockRepository.findAllById(any()))
                .thenReturn(List.of(Stock.create(product1.getId(), 3)));

        List<ProductWithStock> result = productApplicationService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).product()).isSameAs(product1);
        assertThat(result.get(0).stock()).isEqualTo(3);
        assertThat(result.get(1).product()).isSameAs(product2);
        assertThat(result.get(1).stock()).isNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(stockRepository, times(1)).findAllById(captor.capture());
        assertThat(captor.getValue()).containsExactly(product1.getId(), product2.getId());
        verify(stockRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("getAll: 상품이 없으면 빈 목록을 반환하고 재고는 조회하지 않는다")
    void getAll_withNoProducts_returnsEmptyListWithoutStockQuery() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertThat(productApplicationService.getAll()).isEmpty();

        verify(stockRepository, never()).findAllById(any());
    }
}
