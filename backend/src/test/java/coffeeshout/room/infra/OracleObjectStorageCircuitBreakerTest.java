package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.global.config.properties.OracleObjectStorageProperties;
import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.exception.custom.StorageServiceException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OracleObjectStorageCircuitBreakerTest {

    @Mock
    ObjectStorage objectStorage;

    @Mock
    PutObjectResponse putObjectResponse;

    OracleObjectStorageService storageService;
    CircuitBreaker circuitBreaker;
    Retry retry;

    @BeforeEach
    void setUp() {
        OracleObjectStorageProperties oracleProperties = new OracleObjectStorageProperties(
                "ap-chuncheon-1",
                "test-namespace",
                "test-bucket"
        );
        QrProperties qrProperties = new QrProperties(
                "https://example.com/join",
                150,
                150,
                new QrProperties.PresignedUrl(1),
                "qr-code"
        );

        storageService = new OracleObjectStorageService(
                objectStorage,
                oracleProperties,
                qrProperties,
                new SimpleMeterRegistry()
        );

        // 서킷 브레이커 설정
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("oracleStorage");

        // 리트라이 설정
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(RuntimeException.class)
                .build();

        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        retry = retryRegistry.retry("oracleStorage");
    }

    @Test
    @DisplayName("정상 상태에서는 서킷이 CLOSED 상태를 유지한다")
    void 정상_상태에서_서킷_CLOSED() {
        // given
        when(objectStorage.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResponse);
        when(putObjectResponse.getETag()).thenReturn("test-etag");

        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> storageService.upload("test", "data".getBytes())
        );

        // when
        decoratedSupplier.get();

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("실패율이 임계치를 넘으면 서킷이 OPEN 상태가 된다")
    void 실패율_초과시_서킷_OPEN() {
        // given
        when(objectStorage.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("Storage unavailable"));

        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> storageService.upload("test", "data".getBytes())
        );

        // when - minimumNumberOfCalls(5)의 50% 이상 실패해야 OPEN
        for (int i = 0; i < 5; i++) {
            try {
                decoratedSupplier.get();
            } catch (Exception ignored) {
            }
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("서킷이 OPEN 상태일 때 요청이 즉시 차단된다")
    void 서킷_OPEN시_즉시_차단() {
        // given
        circuitBreaker.transitionToOpenState();

        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> storageService.upload("test", "data".getBytes())
        );

        // when & then
        assertThatThrownBy(decoratedSupplier::get)
                .isInstanceOf(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);

        // ObjectStorage가 호출되지 않았는지 확인
        verify(objectStorage, times(0)).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("일시적 실패 시 리트라이가 동작한다")
    void 일시적_실패시_리트라이() {
        // given - 2번 실패 후 성공
        when(objectStorage.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("Temporary failure"))
                .thenThrow(new RuntimeException("Temporary failure"))
                .thenReturn(putObjectResponse);
        when(putObjectResponse.getETag()).thenReturn("test-etag");

        Supplier<String> decoratedSupplier = Retry.decorateSupplier(
                retry,
                () -> storageService.upload("test", "data".getBytes())
        );

        // when
        String result = decoratedSupplier.get();

        // then - 3번 호출됨 (2번 실패 + 1번 성공)
        verify(objectStorage, times(3)).putObject(any(PutObjectRequest.class));
        assertThat(result).isEqualTo("qr-code/test.png");
    }

    @Test
    @DisplayName("리트라이 횟수를 초과하면 예외가 발생한다")
    void 리트라이_초과시_예외() {
        // given - 계속 실패
        when(objectStorage.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("Persistent failure"));

        Supplier<String> decoratedSupplier = Retry.decorateSupplier(
                retry,
                () -> storageService.upload("test", "data".getBytes())
        );

        // when & then
        assertThatThrownBy(decoratedSupplier::get)
                .isInstanceOf(StorageServiceException.class);

        // maxAttempts(3)만큼 호출됨
        verify(objectStorage, times(3)).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("서킷 브레이커와 리트라이를 함께 사용할 수 있다")
    void 서킷브레이커_리트라이_조합() {
        // given - 2번 실패 후 성공
        when(objectStorage.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("Temporary failure"))
                .thenThrow(new RuntimeException("Temporary failure"))
                .thenReturn(putObjectResponse);
        when(putObjectResponse.getETag()).thenReturn("test-etag");

        // 서킷 브레이커 안에 리트라이를 감싸서 사용
        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(
                        retry,
                        () -> storageService.upload("test", "data".getBytes())
                )
        );

        // when
        String result = decoratedSupplier.get();

        // then
        assertThat(result).isEqualTo("qr-code/test.png");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verify(objectStorage, times(3)).putObject(any(PutObjectRequest.class));
    }
}
