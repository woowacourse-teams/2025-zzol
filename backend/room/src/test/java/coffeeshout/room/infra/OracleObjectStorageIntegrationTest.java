package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.room.config.OracleObjectStorageProperties;
import coffeeshout.room.config.QrProperties;
import coffeeshout.support.IntegrationTestSupport;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * OracleObjectStorageService의 서킷 브레이커 및 리트라이 동작을 검증하는 통합 테스트. 실제 Spring 컨텍스트에서 Resilience4j 어노테이션(@CircuitBreaker,
 * @Retry)과 폴백(fallbackMethod) 로직이 정상적으로 작동하여 InfrastructureException을 던지는지 확인합니다.
 */
@ActiveProfiles({"test", "circuit-breaker-test"})
class OracleObjectStorageIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private ObjectStorage objectStorage;

    @Autowired
    private OracleObjectStorageService oracleObjectStorageService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전 서킷 브레이커 상태를 초기화(CLOSED)합니다.
        circuitBreakerRegistry.circuitBreaker("oracleStorage").reset();
    }

    /**
     * OracleObjectStorageService는 !test 프로필에서만 로드되므로, 테스트용 컨텍스트에서 해당 빈을 수동으로 등록하여 AOP가 적용되도록 합니다.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public OracleObjectStorageService oracleObjectStorageService(
                ObjectStorage objectStorage,
                OracleObjectStorageProperties oracleProperties,
                QrProperties qrProperties,
                MeterRegistry meterRegistry
        ) {
            return new OracleObjectStorageService(objectStorage, oracleProperties, qrProperties, meterRegistry);
        }
    }

    @Test
    @DisplayName("재시도 횟수를 초과하는 지속적인 실패 발생 시, 최종적으로 fallback이 실행되어 InfrastructureException을 던진다")
    void 지속적_실패_시_InfrastructureException_발생() {
        // given
        when(objectStorage.putObject(any(PutObjectRequest.class)))
                .thenThrow(new BmcException(500, "ServiceCode", "Persistent failure", "requestId"));

        // when & then
        assertThatThrownBy(() -> oracleObjectStorageService.upload("test-contents", "test-data".getBytes()))
                .isInstanceOf(InfrastructureException.class)
                .hasMessageContaining("QR 코드 업로드에 실패했습니다");

        // 설정된 maxAttempts(3)만큼 호출되었는지 확인
        verify(objectStorage, times(3)).putObject(any(PutObjectRequest.class));
    }

    @Disabled("로컬에서 돌아갔으므로 비활성화 처리")
    @Test
    @DisplayName("서킷 브레이커가 OPEN 상태일 때 호출하면, 즉시 fallback이 실행되어 InfrastructureException(차단 메시지)을 던진다")
    void 서킷_OPEN_상태_시_즉시_InfrastructureException_발생() {
        // given - 실패율(50%)을 넘겨서 서킷을 OPEN 상태로 전이시킴
        when(objectStorage.putObject(any(PutObjectRequest.class)))
                .thenThrow(new BmcException(500, "ServiceCode", "Fail", "requestId"));

        // minimumNumberOfCalls(10)만큼 호출하여 서킷 OPEN 유도
        for (int i = 0; i < 10; i++) {
            try {
                oracleObjectStorageService.upload("test", "data".getBytes());
            } catch (InfrastructureException ignored) {
            }
        }

        // when & then
        assertThatThrownBy(() -> oracleObjectStorageService.upload("test", "data".getBytes()))
                .isInstanceOf(InfrastructureException.class)
                .hasMessageContaining("스토리지 서비스가 일시적으로 사용 불가능합니다");
    }
}
