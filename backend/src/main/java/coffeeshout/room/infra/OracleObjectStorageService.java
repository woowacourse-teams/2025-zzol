package coffeeshout.room.infra;

import coffeeshout.global.config.properties.OracleObjectStorageProperties;
import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.exception.custom.StorageServiceException;
import coffeeshout.room.application.port.StorageService;
import coffeeshout.room.domain.RoomErrorCode;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
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
        // objectName 검증
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("objectName은 null이거나 비어있을 수 없습니다.");
        }

        // Public 버킷이므로 PAR 없이 직접 URL 생성
        final String publicUrl = String.format("https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                region, namespaceName, bucketName, objectName);

        log.info("Public URL 생성 완료: objectName={}", objectName);
        meterRegistry.counter("oracle.objectstorage.url.generation.success").increment();

        return publicUrl;
    }
}
