package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.global.config.properties.OracleObjectStorageProperties;
import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.exception.custom.StorageServiceException;
import coffeeshout.room.domain.RoomErrorCode;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OracleObjectStorageServiceTest {

    @Mock
    ObjectStorage objectStorage;

    @Mock
    PutObjectResponse putObjectResponse;

    MeterRegistry meterRegistry;

    OracleObjectStorageService oracleObjectStorageService;

    OracleObjectStorageProperties oracleProperties;
    QrProperties qrProperties;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        oracleProperties = new OracleObjectStorageProperties(
                "ap-chuncheon-1",
                "ax9wq4bhn4cn",
                "zzol-public"
        );
        qrProperties = new QrProperties(
                "https://example.com/join",
                150,
                150,
                new QrProperties.PresignedUrl(1),
                "qr-code"
        );

        oracleObjectStorageService = new OracleObjectStorageService(
                objectStorage,
                oracleProperties,
                qrProperties,
                meterRegistry
        );
    }

    @Test
    void QR_코드_업로드가_성공적으로_완료된다() {
        // given
        String contents = "ABC123";
        byte[] qrCodeImage = "mock qr code image".getBytes();
        String expectedStorageKey = "qr-code/ABC123.png";

        when(objectStorage.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResponse);
        when(putObjectResponse.getETag()).thenReturn("mock-etag");

        // when
        String storageKey = oracleObjectStorageService.upload(contents, qrCodeImage);

        // then
        assertThat(storageKey).isEqualTo(expectedStorageKey);
        verify(objectStorage).putObject(any(PutObjectRequest.class));

        // 메트릭 검증
        Counter uploadSuccessCounter = meterRegistry.find("oracle.objectstorage.upload.success").counter();
        assertThat(uploadSuccessCounter).isNotNull();
        assertThat(uploadSuccessCounter.count()).isEqualTo(1.0);
    }

    @Test
    void 업로드_시_올바른_메타데이터로_요청을_생성한다() {
        // given
        String contents = "TEST456";
        byte[] qrCodeImage = "test image data".getBytes();

        when(objectStorage.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResponse);
        when(putObjectResponse.getETag()).thenReturn("mock-etag");

        // when
        oracleObjectStorageService.upload(contents, qrCodeImage);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(objectStorage).putObject(requestCaptor.capture());

        PutObjectRequest capturedRequest = requestCaptor.getValue();

        assertSoftly(softly -> {
            softly.assertThat(capturedRequest.getNamespaceName()).isEqualTo("ax9wq4bhn4cn");
            softly.assertThat(capturedRequest.getBucketName()).isEqualTo("zzol-public");
            softly.assertThat(capturedRequest.getObjectName()).isEqualTo("qr-code/TEST456.png");
            softly.assertThat(capturedRequest.getContentType()).isEqualTo("image/png");
            softly.assertThat(capturedRequest.getContentLength()).isEqualTo((long) qrCodeImage.length);
        });
    }

    @Test
    void 빈_데이터_업로드_시_예외를_던진다() {
        // given
        String contents = "EMPTY";
        byte[] emptyData = new byte[0];

        // when & then
        assertThatThrownBy(() -> oracleObjectStorageService.upload(contents, emptyData))
                .isInstanceOf(StorageServiceException.class)
                .hasMessageContaining("QR 이미지 바이트가 비어 있습니다.");
    }

    @Test
    void 업로드_실패_시_StorageServiceException을_던지고_실패_메트릭을_기록한다() {
        // given
        String contents = "FAIL123";
        byte[] qrCodeImage = "mock data".getBytes();

        when(objectStorage.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("Upload failed"));

        // when & then
        assertThatThrownBy(() -> oracleObjectStorageService.upload(contents, qrCodeImage))
                .isInstanceOf(StorageServiceException.class)
                .hasMessageContaining(RoomErrorCode.QR_CODE_UPLOAD_FAILED.getMessage());

        // 실패 메트릭 검증
        Counter uploadFailedCounter = meterRegistry.find("oracle.objectstorage.qr.upload.failed")
                .tag("error", "RuntimeException")
                .counter();
        assertThat(uploadFailedCounter).isNotNull();
        assertThat(uploadFailedCounter.count()).isEqualTo(1.0);
    }

    @Test
    void Public_URL_생성이_성공적으로_완료된다() {
        // given
        String storageKey = "qr-code/XYZ789.png";
        String expectedUrl = "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/ax9wq4bhn4cn/b/zzol-public/o/qr-code/XYZ789.png";

        // when
        String url = oracleObjectStorageService.getUrl(storageKey);

        // then
        assertThat(url).isEqualTo(expectedUrl);

        // 메트릭 검증
        Counter urlGenerationSuccessCounter = meterRegistry.find("oracle.objectstorage.url.generation.success").counter();
        assertThat(urlGenerationSuccessCounter).isNotNull();
        assertThat(urlGenerationSuccessCounter.count()).isEqualTo(1.0);
    }

    @Test
    void objectName이_null이면_예외를_던진다() {
        // given
        String storageKey = null;

        // when & then
        assertThatThrownBy(() -> oracleObjectStorageService.getUrl(storageKey))
                .isInstanceOf(StorageServiceException.class)
                .hasMessageContaining(RoomErrorCode.QR_CODE_URL_SIGNING_FAILED.getMessage());

        // 실패 메트릭 검증
        Counter urlGenerationFailedCounter = meterRegistry.find("oracle.objectstorage.qr.url.generation.failed")
                .tag("error", "IllegalArgumentException")
                .counter();
        assertThat(urlGenerationFailedCounter).isNotNull();
        assertThat(urlGenerationFailedCounter.count()).isEqualTo(1.0);
    }

    @Test
    void objectName이_빈_문자열이면_예외를_던진다() {
        // given
        String storageKey = "   ";

        // when & then
        assertThatThrownBy(() -> oracleObjectStorageService.getUrl(storageKey))
                .isInstanceOf(StorageServiceException.class)
                .hasMessageContaining(RoomErrorCode.QR_CODE_URL_SIGNING_FAILED.getMessage());

        // 실패 메트릭 검증
        Counter urlGenerationFailedCounter = meterRegistry.find("oracle.objectstorage.qr.url.generation.failed")
                .tag("error", "IllegalArgumentException")
                .counter();
        assertThat(urlGenerationFailedCounter).isNotNull();
        assertThat(urlGenerationFailedCounter.count()).isEqualTo(1.0);
    }

    @Test
    void Public_URL_형식이_올바르게_생성된다() {
        // given
        String storageKey = "qr-code/FORMAT_TEST.png";

        // when
        String url = oracleObjectStorageService.getUrl(storageKey);

        // then
        assertThat(url).startsWith("https://")
                .contains("objectstorage")
                .contains("ap-chuncheon-1")
                .contains("oraclecloud.com")
                .contains("ax9wq4bhn4cn") // namespace
                .contains("zzol-public") // bucket
                .endsWith(storageKey);
    }
}
