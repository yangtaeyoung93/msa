package com.example.msa.payment.infrastructure.gateway;

import com.example.msa.payment.application.exception.UnsupportedPaymentProviderException;
import com.example.msa.payment.domain.model.PaymentProvider;
import com.example.msa.payment.infrastructure.gateway.testpayment.TestPaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentGatewayRegistryTest {

    @Test
    @DisplayName("provider에 맞는 게이트웨이를 찾아준다")
    void get_returnsGatewayOfProvider() {
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of(new TestPaymentGateway()));

        assertThat(registry.get(PaymentProvider.TEST)).isInstanceOf(TestPaymentGateway.class);
    }

    @Test
    @DisplayName("등록된 게이트웨이가 없으면 UnsupportedPaymentProviderException")
    void get_unregisteredProvider_throws() {
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of());

        assertThatThrownBy(() -> registry.get(PaymentProvider.TEST))
                .isInstanceOf(UnsupportedPaymentProviderException.class);
    }

    @Test
    @DisplayName("같은 provider의 게이트웨이가 중복 등록되면 IllegalStateException")
    void constructor_duplicateProvider_throws() {
        assertThatThrownBy(() -> new PaymentGatewayRegistry(
                List.of(new TestPaymentGateway(), new TestPaymentGateway())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("PaymentProvider.from: 대소문자와 공백을 무시하고 변환한다")
    void providerFrom_ignoresCaseAndWhitespace() {
        assertThat(PaymentProvider.from(" test ")).isEqualTo(PaymentProvider.TEST);
        assertThat(PaymentProvider.from("Test")).isEqualTo(PaymentProvider.TEST);
    }

    @Test
    @DisplayName("PaymentProvider.from: null/빈 문자열/알 수 없는 값은 UnsupportedPaymentProviderException")
    void providerFrom_invalid_throws() {
        assertThatThrownBy(() -> PaymentProvider.from(null)).isInstanceOf(UnsupportedPaymentProviderException.class);
        assertThatThrownBy(() -> PaymentProvider.from(" ")).isInstanceOf(UnsupportedPaymentProviderException.class);
        assertThatThrownBy(() -> PaymentProvider.from("TOSS")).isInstanceOf(UnsupportedPaymentProviderException.class);
    }
}
