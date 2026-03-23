package coffeeshout.room.infra;

import coffeeshout.global.config.properties.OracleObjectStorageProperties;
import coffeeshout.room.config.QrProperties;
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

    /**
     * QR 코드 이미지를 Oracle Object Storage에 업로드합니다.
     * 
     * 서킷 브레이커와 리트라이가 적용되어 있으며, 원본 예외를 그대로 던져서
     * Resilience4j가 예외 타입을 정확히 판단할 수 있도록 합니다.
     * 모든 예외 래핑은 fallback 메서드에서 처리합니다.
     */
    @Override
    @CircuitBreaker(name = "oracleStorage", fallbackMethod = "uploadFallback")
    @Retry(name = "oracleStorage")
    public String upload(@NonNull String contents, byte[] data) {
        if (data.length == 0) {
            throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED, "QR 이미지 바이트가 비어 있습니다.");
        }

        return doUpload(contents, data);
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
     * 서킷 브레이커 OPEN 시 또는 모든 재시도 실패 시 호출되는 폴백 메서드.
     * 여기서 예외를 StorageServiceException으로 래핑합니다.
     */
    private String uploadFallback(String contents, byte[] data, Exception e) {
        meterRegistry.counter("oracle.objectstorage.qr.upload.failed",
                "error", e.getClass().getSimpleName()).increment();

        if (e instanceof CallNotPermittedException) {
            log.warn("서킷 브레이커 OPEN 상태 - Oracle Storage 호출 차단됨: contents={}", contents);
            throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED,
                    "스토리지 서비스가 일시적으로 사용 불가능합니다. 잠시 후 다시 시도해주세요.");
        }

        log.error("Oracle Object Storage QR 코드 업로드 실패: contents={}, error={}", contents, e.getMessage(), e);
        throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED,
                "QR 코드 업로드에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
    }

    /**
     * 실제 업로드 로직. 예외를 래핑하지 않고 그대로 던집니다.
     */
    private String doUpload(String contents, byte[] qrCodeImage) {
        try {
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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
