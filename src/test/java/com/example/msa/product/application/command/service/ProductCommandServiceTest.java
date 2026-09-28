package com.example.msa.product.application.command.service;

import com.example.msa.presentaion.dto.request.ProductCreateRequest;
import com.example.msa.presentaion.dto.request.ProductUpdateRequest;
import com.example.msa.product.application.exception.ProductNotfoundException;
import com.example.msa.product.domain.model.Product;
import com.example.msa.product.domain.repository.command.ProductCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {

    @Mock
    private ProductCommandRepository productCommandRepository;

    @InjectMocks
    private ProductCommandService productCommandService;

    @Test
    @DisplayName("create: 유효한 요청이면 상품을 생성하고 저장한다")
    void create_withValidRequest_savesProduct() {
        ProductCreateRequest request = new ProductCreateRequest(
                UUID.randomUUID().toString(),
                "상품명",
                "설명",
                BigDecimal.valueOf(1000),
                10,
                "ACTIVE",
                UUID.randomUUID().toString()
        );
        when(productCommandRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productCommandService.create(request);

        assertThat(result.getName()).isEqualTo("상품명");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(productCommandRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("create: sellerId가 UUID 형식이 아니면 BAD_REQUEST 예외가 발생한다")
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

        assertThatThrownBy(() -> productCommandService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("sellerId");
    }

    @Test
    @DisplayName("create: creatorId가 UUID 형식이 아니면 BAD_REQUEST 예외가 발생한다")
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

        assertThatThrownBy(() -> productCommandService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("creatorId");
    }

    @Test
    @DisplayName("update: 존재하는 상품이면 필드를 갱신한다")
    void update_withExistingProduct_updatesFields() {
        UUID productId = UUID.randomUUID();
        Product existing = Product.create(
                UUID.randomUUID(), "기존이름", "기존설명",
                BigDecimal.valueOf(500), 5, "ACTIVE", UUID.randomUUID()
        );
        when(productCommandRepository.findById(productId)).thenReturn(Optional.of(existing));

        ProductUpdateRequest request = new ProductUpdateRequest(
                "새이름", "새설명", BigDecimal.valueOf(2000), 20, "INACTIVE",
                UUID.randomUUID().toString()
        );

        Product result = productCommandService.update(productId, request);

        assertThat(result.getName()).isEqualTo("새이름");
        assertThat(result.getDescription()).isEqualTo("새설명");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(result.getStock()).isEqualTo(20);
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("update: 존재하지 않는 productId면 ProductNotfoundException이 발생한다")
    void update_withNonExistingProduct_throwsNotFound() {
        UUID productId = UUID.randomUUID();
        when(productCommandRepository.findById(productId)).thenReturn(Optional.empty());

        ProductUpdateRequest request = new ProductUpdateRequest(
                "새이름", "새설명", BigDecimal.valueOf(2000), 20, "INACTIVE",
                UUID.randomUUID().toString()
        );

        assertThatThrownBy(() -> productCommandService.update(productId, request))
                .isInstanceOf(ProductNotfoundException.class);
    }

    @Test
    @DisplayName("update: modifierId가 UUID 형식이 아니면 BAD_REQUEST 예외가 발생한다")
    void update_withInvalidModifierId_throwsBadRequest() {
        UUID productId = UUID.randomUUID();
        Product existing = Product.create(
                UUID.randomUUID(), "기존이름", "기존설명",
                BigDecimal.valueOf(500), 5, "ACTIVE", UUID.randomUUID()
        );
        when(productCommandRepository.findById(productId)).thenReturn(Optional.of(existing));

        ProductUpdateRequest request = new ProductUpdateRequest(
                "새이름", "새설명", BigDecimal.valueOf(2000), 20, "INACTIVE",
                "invalid-uuid"
        );

        assertThatThrownBy(() -> productCommandService.update(productId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("modifierId");
    }

    @Test
    @DisplayName("delete: 존재하는 상품이면 삭제한다")
    void delete_withExistingProduct_deletesProduct() {
        UUID productId = UUID.randomUUID();
        Product existing = Product.create(
                UUID.randomUUID(), "기존이름", "기존설명",
                BigDecimal.valueOf(500), 5, "ACTIVE", UUID.randomUUID()
        );
        when(productCommandRepository.findById(productId)).thenReturn(Optional.of(existing));

        productCommandService.delete(productId);

        verify(productCommandRepository, times(1)).delete(existing);
    }

    @Test
    @DisplayName("delete: 존재하지 않는 productId면 ProductNotfoundException이 발생하고 delete는 호출되지 않는다")
    void delete_withNonExistingProduct_throwsNotFound() {
        UUID productId = UUID.randomUUID();
        when(productCommandRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productCommandService.delete(productId))
                .isInstanceOf(ProductNotfoundException.class);

        verify(productCommandRepository, never()).delete(any(Product.class));
    }
}
