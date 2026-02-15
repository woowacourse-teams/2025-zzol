package coffeeshout.room.infra;

import coffeeshout.global.config.properties.OracleObjectStorageProperties;
import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.exception.custom.StorageServiceException;
import coffeeshout.room.application.port.StorageService;
import coffeeshout.room.domain.RoomErrorCode;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.ByteArrayInputStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!local & !test")
public class OracleObjectStorageService implements StorageService {

    private final ObjectStorage objectStorage;
    private final String namespaceName;
    private final String bucketName;
    private final String region;
    private final MeterRegistry meterRegistry;
    private final Timer uploadTimer;
    private final String storageKeyPrefix;

    public OracleObjectStorageService(
            ObjectStorage objectStorage,
            OracleObjectStorageProperties oracleProperties,
            QrProperties qrProperties,
            MeterRegistry meterRegistry
    ) {
        this.objectStorage = objectStorage;
        this.namespaceName = oracleProperties.namespace();
        this.bucketName = oracleProperties.bucket();
        this.region = oracleProperties.region();
        this.meterRegistry = meterRegistry;
        this.uploadTimer = Timer.builder("oracle.objectstorage.upload.time")
                .description("Time taken to upload QR code to Oracle Object Storage")
                .register(meterRegistry);
        this.storageKeyPrefix = qrProperties.storageKeyPrefix();
    }

    @Override
    @CircuitBreaker(name = "oracleStorage", fallbackMethod = "uploadFallback")
    @Retry(name = "oracleStorage")
    public String upload(@NonNull String contents, byte[] data) {
        if (data.length == 0) {
            throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED, "QR 이미지 바이트가 비어 있습니다.");
        }

        try {
            return uploadQrCodeToObjectStorage(contents, data);
        } catch (Exception e) {
            meterRegistry.counter("oracle.objectstorage.qr.upload.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("Oracle Object Storage QR 코드 업로드 실패: contents={}, error={}", contents, e.getMessage(), e);
            throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED,
                    RoomErrorCode.QR_CODE_UPLOAD_FAILED.getMessage(), e);
        }
    }

    @Override
    public String getUrl(String storageKey) {
        try {
            return generatePublicUrl(storageKey);
        } catch (Exception e) {
            meterRegistry.counter("oracle.objectstorage.qr.url.generation.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("Oracle Object Storage Public URL 생성 실패: storageKey={}, error={}", storageKey, e.getMessage(), e);
            throw new StorageServiceException(RoomErrorCode.QR_CODE_URL_SIGNING_FAILED,
                    RoomErrorCode.QR_CODE_URL_SIGNING_FAILED.getMessage());
        }
    }

    /**
     * 서킷 브레이커 OPEN 시 또는 모든 재시도 실패 시 호출되는 폴백 메서드
     */
    private String uploadFallback(String contents, byte[] data, Exception e) {
        meterRegistry.counter("oracle.objectstorage.circuitbreaker.fallback",
                "error", e.getClass().getSimpleName()).increment();

        if (e instanceof CallNotPermittedException) {
            log.warn("서킷 브레이커 OPEN 상태 - Oracle Storage 호출 차단됨: contents={}", contents);
            throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED,
                    "스토리지 서비스가 일시적으로 사용 불가능합니다. 잠시 후 다시 시도해주세요.");
        }

        log.warn("Oracle Storage 업로드 폴백 처리: contents={}, error={}", contents, e.getMessage());
        throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED,
                "QR 코드 업로드에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
    }

    private String uploadQrCodeToObjectStorage(String contents, byte[] qrCodeImage) throws Exception {
        return uploadTimer.recordCallable(() -> {
            final String objectName = storageKeyPrefix + "/" + contents + ".png";

            final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .namespaceName(namespaceName)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .contentType("image/png")
                    .contentLength((long) qrCodeImage.length)
                    .putObjectBody(new ByteArrayInputStream(qrCodeImage))
                    .build();

            final PutObjectResponse response = objectStorage.putObject(putObjectRequest);
            log.info("QR 코드 Oracle Object Storage 업로드 완료: contents={}, objectName={}, etag={}",
                    contents, objectName, response.getETag());

            meterRegistry.counter("oracle.objectstorage.upload.success").increment();
            return objectName;
        });
    }

    private String generatePublicUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("objectName은 null이거나 비어있을 수 없습니다.");
        }

        final String publicUrl = String.format("https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                region, namespaceName, bucketName, objectName);

        log.info("Public URL 생성 완료: objectName={}", objectName);
        meterRegistry.counter("oracle.objectstorage.url.generation.success").increment();

        return publicUrl;
    }
}
