package coffeeshout.room.infra;

import coffeeshout.global.config.properties.OracleObjectStorageProperties;
import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.exception.custom.StorageServiceException;
import coffeeshout.room.application.StorageService;
import coffeeshout.room.domain.RoomErrorCode;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
public class OracleObjectStorageService implements StorageService {

    private final ObjectStorage objectStorage;
    private final String namespaceName;
    private final String bucketName;
    private final String region;
    private final int presignedUrlExpirationHours;
    private final MeterRegistry meterRegistry;
    private final Timer uploadTimer;
    private final Timer presignerTimer;
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
        this.presignedUrlExpirationHours = qrProperties.presignedUrl().expirationHours();
        this.meterRegistry = meterRegistry;
        this.uploadTimer = Timer.builder("oracle.objectstorage.upload.time")
                .description("Time taken to upload QR code to Oracle Object Storage")
                .register(meterRegistry);
        this.presignerTimer = Timer.builder("oracle.objectstorage.presigner.time")
                .description("Time taken to generate presigned URL")
                .register(meterRegistry);
        this.storageKeyPrefix = qrProperties.s3KeyPrefix();
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
            throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED, RoomErrorCode.QR_CODE_UPLOAD_FAILED.getMessage(), e);
        }
    }

    @Override
    public String getUrl(String storageKey) {
        try {
            return generatePresignedUrl(storageKey);
        } catch (Exception e) {
            meterRegistry.counter("oracle.objectstorage.qr.url.signing.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("Oracle Object Storage Presigned URL 생성 실패: storageKey={}, error={}", storageKey, e.getMessage(), e);
            throw new StorageServiceException(RoomErrorCode.QR_CODE_URL_SIGNING_FAILED,
                    RoomErrorCode.QR_CODE_URL_SIGNING_FAILED.getMessage());
        }
    }

    private String uploadQrCodeToObjectStorage(String contents, byte[] qrCodeImage) throws Exception {
        return uploadTimer.recordCallable(() -> {
            String objectName = storageKeyPrefix + "/" + contents + ".png";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .namespaceName(namespaceName)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .contentType("image/png")
                    .contentLength((long) qrCodeImage.length)
                    .putObjectBody(new ByteArrayInputStream(qrCodeImage))
                    .build();

            PutObjectResponse response = objectStorage.putObject(putObjectRequest);
            log.info("QR 코드 Oracle Object Storage 업로드 완료: contents={}, objectName={}, etag={}",
                    contents, objectName, response.getETag());

            meterRegistry.counter("oracle.objectstorage.upload.success").increment();
            return objectName;
        });
    }

    private String generatePresignedUrl(String objectName) throws Exception {
        return presignerTimer.recordCallable(() -> {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .namespaceName(namespaceName)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .build();

            // Oracle Object Storage Presigned URL 생성
            // 만료 시간 계산
            Date expirationDate = new Date(System.currentTimeMillis() +
                    Duration.ofHours(presignedUrlExpirationHours).toMillis());

            URL presignedUrl = objectStorage.createPreauthenticatedRequest(
                    com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest.builder()
                            .namespaceName(namespaceName)
                            .bucketName(bucketName)
                            .createPreauthenticatedRequestDetails(
                                    com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails.builder()
                                            .name("qr-" + System.currentTimeMillis())
                                            .objectName(objectName)
                                            .accessType(com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                                            .timeExpires(expirationDate)
                                            .build()
                            )
                            .build()
            ).getCreatePreauthenticatedRequestResponse().getPreauthenticatedRequest().getAccessUri();

            // Full URL 생성: https://<namespace>.objectstorage.<region>.oci.customer-oci.com<accessUri>
            String fullUrl = String.format("https://%s.objectstorage.%s.oci.customer-oci.com%s",
                    namespaceName, region, presignedUrl);

            log.info("Presigned URL 생성 완료: objectName={}, expiresInHours={}", objectName, presignedUrlExpirationHours);
            meterRegistry.counter("oracle.objectstorage.url.signing.success").increment();

            return fullUrl;
        });
    }
}
