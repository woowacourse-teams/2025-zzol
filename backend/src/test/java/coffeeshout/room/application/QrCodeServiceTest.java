package coffeeshout.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.exception.custom.QRCodeGenerationException;
import coffeeshout.global.exception.custom.StorageServiceException;
import coffeeshout.room.domain.QrCodeStatus;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.service.QrCodeGenerator;
import coffeeshout.room.infra.messaging.RoomEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @Mock
    QrCodeGenerator qrCodeGenerator;

    @Mock
    StorageService storageService;

    @Mock
    RoomEventPublisher roomEventPublisher;

    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    QrCodeService qrCodeService;

    String qrCodePrefix = "https://example.com/join";

    @BeforeEach
    void setUp() {
        QrProperties qrProperties = new QrProperties(qrCodePrefix, 10, 100, null, "");
        qrCodeService = new QrCodeService(
                qrProperties,
                qrCodeGenerator,
                storageService,
                meterRegistry,
                roomEventPublisher
        );
    }

    @Test
    void QR_코드_URL_생성이_성공적으로_완료된다() throws Exception {
        // given
        String contents = "TXXX";
        byte[] qrCodeImage = "mock qr code image".getBytes();
        String storageKey = "qr-code/TXXX/uuid.png";
        String expectedUrl = "https://storage.example.com/qr-code/TXXX/uuid.png";

        when(qrCodeGenerator.generate(anyString())).thenReturn(qrCodeImage);
        when(storageService.upload(contents, qrCodeImage)).thenReturn(storageKey);
        when(storageService.getUrl(storageKey)).thenReturn(expectedUrl);

        // when
        String result = qrCodeService.getQrCodeUrl(contents);

        // then
        assertThat(result).isEqualTo(expectedUrl);

        verify(qrCodeGenerator).generate("https://example.com/join/TXXX");
        verify(storageService).upload(contents, qrCodeImage);
        verify(storageService).getUrl(storageKey);
    }

    @Test
    void QR_코드_생성_실패_시_QR_CODE_GENERATION_FAILED_에러를_던진다() throws Exception {
        // given
        String contents = "TXXX";
        when(qrCodeGenerator.generate(anyString()))
                .thenThrow(new RuntimeException("QR code generation failed"));

        // when & then
        assertThatThrownBy(() -> qrCodeService.getQrCodeUrl(contents))
                .isInstanceOf(QRCodeGenerationException.class)
                .hasMessageContaining(RoomErrorCode.QR_CODE_GENERATION_FAILED.getMessage());
    }

    @Test
    void 스토리지_업로드_실패_시_해당_예외를_그대로_전파한다_503_에러() throws Exception {
        // given
        String contents = "TXXX";
        byte[] qrCodeImage = "mock qr code image".getBytes();

        when(qrCodeGenerator.generate(anyString())).thenReturn(qrCodeImage);
        when(storageService.upload(contents, qrCodeImage))
                .thenThrow(new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED, RoomErrorCode.QR_CODE_UPLOAD_FAILED.getMessage()));

        // when & then
        assertThatThrownBy(() -> qrCodeService.getQrCodeUrl(contents))
                .isInstanceOf(StorageServiceException.class)
                .hasMessageContaining(RoomErrorCode.QR_CODE_UPLOAD_FAILED.getMessage());
    }

    @Test
    void URL_생성_실패_시_해당_예외를_그대로_전파한다_503_에러() throws Exception {
        // given
        String contents = "TXXX";
        byte[] qrCodeImage = "mock qr code image".getBytes();
        String storageKey = "qr-code/TXXX/uuid.png";

        when(qrCodeGenerator.generate(anyString())).thenReturn(qrCodeImage);
        when(storageService.upload(contents, qrCodeImage)).thenReturn(storageKey);
        when(storageService.getUrl(storageKey))
                .thenThrow(new StorageServiceException(RoomErrorCode.QR_CODE_URL_SIGNING_FAILED,
                        RoomErrorCode.QR_CODE_URL_SIGNING_FAILED.getMessage()));

        // when & then
        assertThatThrownBy(() -> qrCodeService.getQrCodeUrl(contents))
                .isInstanceOf(StorageServiceException.class)
                .hasMessageContaining(RoomErrorCode.QR_CODE_URL_SIGNING_FAILED.getMessage());
    }

    @Test
    void QR_코드_생성_시_올바른_URL을_사용한다() throws Exception {
        // given
        String contents = "TXXX";
        byte[] qrCodeImage = "mock qr code image".getBytes();
        String storageKey = "qr-code/TXXX/uuid.png";
        String expectedUrl = "https://storage.example.com/qr-code/TXXX/uuid.png";

        when(qrCodeGenerator.generate(anyString())).thenReturn(qrCodeImage);
        when(storageService.upload(contents, qrCodeImage)).thenReturn(storageKey);
        when(storageService.getUrl(storageKey)).thenReturn(expectedUrl);

        // when
        qrCodeService.getQrCodeUrl(contents);

        // then
        verify(qrCodeGenerator).generate("https://example.com/join/TXXX");
    }

    @Test
    void 일반_예외_발생_시_QR_CODE_GENERATION_FAILED_에러로_래핑한다() throws Exception {
        // given
        String contents = "TXXX";

        when(qrCodeGenerator.generate(anyString())).thenThrow(new RuntimeException("Unexpected error"));

        // when & then
        assertThatThrownBy(() -> qrCodeService.getQrCodeUrl(contents))
                .isInstanceOf(QRCodeGenerationException.class)
                .hasMessageContaining(RoomErrorCode.QR_CODE_GENERATION_FAILED.getMessage());
    }

    // ===== 비동기 QR 코드 생성 테스트 =====

    @Test
    void 비동기_QR_코드_생성이_성공하면_Redis_pub_sub으로_SUCCESS_이벤트를_발행한다() throws Exception {
        // given
        String joinCode = "TXMK";

        byte[] qrCodeImage = "mock qr code image".getBytes();
        String storageKey = "qr-code/TEST123/uuid.png";
        String expectedUrl = "https://storage.example.com/qr-code/TEST123/uuid.png";

        when(qrCodeGenerator.generate(anyString())).thenReturn(qrCodeImage);
        when(storageService.upload(joinCode, qrCodeImage)).thenReturn(storageKey);
        when(storageService.getUrl(storageKey)).thenReturn(expectedUrl);

        // when
        qrCodeService.generateQrCodeAsync(joinCode);

        // then

        ArgumentCaptor<QrCodeStatusEvent> successEventCaptor = ArgumentCaptor.forClass(QrCodeStatusEvent.class);
        verify(roomEventPublisher).publishEvent(successEventCaptor.capture());

        // 2. 두 번째 이벤트는 SUCCESS (roomEventPublisher를 통해 발행)
        QrCodeStatusEvent successEvent = successEventCaptor.getValue();
        assertThat(successEvent.joinCode()).isEqualTo(joinCode);
        assertThat(successEvent.status()).isEqualTo(QrCodeStatus.SUCCESS);
        assertThat(successEvent.qrCodeUrl()).isEqualTo(expectedUrl);
    }

    @Test
    void 비동기_QR_코드_생성_실패_시_Redis_pub_sub으로_ERROR_이벤트를_발행한다() throws Exception {
        // given
        String joinCode = "TXMK";

        when(qrCodeGenerator.generate(anyString())).thenThrow(new RuntimeException("QR generation failed"));

        // when
        qrCodeService.generateQrCodeAsync(joinCode);

        // then
        ArgumentCaptor<QrCodeStatusEvent> errorCaptor = ArgumentCaptor.forClass(QrCodeStatusEvent.class);
        verify(roomEventPublisher).publishEvent(errorCaptor.capture());

        assertThat(errorCaptor.getValue().status()).isEqualTo(QrCodeStatus.ERROR);
    }
}
