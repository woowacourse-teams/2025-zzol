package coffeeshout.room.application;

import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.exception.custom.QRCodeGenerationException;
import coffeeshout.room.domain.QrCodeStatus;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.service.QrCodeGenerator;
import coffeeshout.room.infra.messaging.RoomEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class QrCodeService {

    private final String qrCodePrefix;
    private final QrCodeGenerator qrCodeGenerator;
    private final StorageService storageService;
    private final MeterRegistry meterRegistry;
    private final Timer qrCodeGenerationTimer;
    private final RoomEventPublisher roomEventPublisher;

    public QrCodeService(
            QrProperties qrProperties,
            QrCodeGenerator qrCodeGenerator,
            StorageService storageService,
            MeterRegistry meterRegistry,
            RoomEventPublisher roomEventPublisher
    ) {
        this.qrCodePrefix = qrProperties.prefix();
        this.qrCodeGenerator = qrCodeGenerator;
        this.storageService = storageService;
        this.meterRegistry = meterRegistry;
        this.qrCodeGenerationTimer = Timer.builder("qr.code.generation.time")
                .description("Time taken to generate QR code")
                .register(meterRegistry);
        this.roomEventPublisher = roomEventPublisher;
    }

    /**
     * QR 코드를 비동기로 생성하고 WebSocket을 통해 상태를 브로드캐스트합니다.
     */
    @Async("qrCodeTaskExecutor")
    public void generateQrCodeAsync(String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            log.warn("유효하지 않은 joinCode: joinCode='{}'", joinCode);
            return;
        }

        log.info("QR 코드 비동기 생성 시작: joinCode={}", joinCode);

        try {
            // 2. QR 코드 생성
            final String qrCodeUrl = getQrCodeUrl(joinCode);

            // 3. Room에 저장
            roomEventPublisher.publishEvent(new QrCodeStatusEvent(joinCode, QrCodeStatus.SUCCESS, qrCodeUrl));
            log.info("QR 코드 생성 완료: joinCode={}, url={}", joinCode, qrCodeUrl);
        } catch (Exception e) {
            log.error("QR 코드 생성 실패: joinCode={}, error={}", joinCode, e.getMessage(), e);

            roomEventPublisher.publishEvent(new QrCodeStatusEvent(joinCode, QrCodeStatus.ERROR, null));
        }
    }

    @Observed(name = "qrcode.generation")
    public String getQrCodeUrl(String contents) {
        try {
            final byte[] qrCodeImage = generateQrCode(contents);
            final String storageKey = uploadToStorage(contents, qrCodeImage);
            return getStorageUrl(storageKey);
        } catch (Exception e) {
            meterRegistry.counter("qr.service.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("QR 이미지 URL 생성 실패: contents={}, error={}", contents, e.getMessage(), e);

            throw e;
        }
    }

    private String uploadToStorage(String contents, byte[] qrCodeImage) {
        return storageService.upload(contents, qrCodeImage);
    }

    private String getStorageUrl(String storageKey) {
        return storageService.getUrl(storageKey);
    }

    private byte[] generateQrCode(String contents) {
        final String url = getUrl(contents);

        try {
            return qrCodeGenerationTimer.recordCallable(() ->
                    qrCodeGenerator.generate(url)
            );
        } catch (Exception e) {
            meterRegistry.counter("qr.generation.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("QR 코드 생성 실패: contents={}, error={}", contents, e.getMessage(), e);

            throw new QRCodeGenerationException(RoomErrorCode.QR_CODE_GENERATION_FAILED, "QR 코드 생성에 실패했습니다.", e);
        }
    }

    private String getUrl(String contents) {
        return UriComponentsBuilder.fromUriString(qrCodePrefix)
                .pathSegment(contents)
                .toUriString();
    }
}
