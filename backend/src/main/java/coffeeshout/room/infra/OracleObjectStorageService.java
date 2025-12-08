package coffeeshout.room.infra;

import coffeeshout.global.config.properties.OracleObjectStorageProperties;
import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.exception.custom.StorageServiceException;
import coffeeshout.room.application.StorageService;
import coffeeshout.room.domain.RoomErrorCode;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Date;
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
            // 만료 시간 계산
            Date expirationDate = new Date(System.currentTimeMillis() +
                    Duration.ofHours(presignedUrlExpirationHours).toMillis());

            // 1. PAR(Pre-Authenticated Request) Details 생성
            CreatePreauthenticatedRequestDetails details =
                    CreatePreauthenticatedRequestDetails.builder()
                            .name("qr-" + objectName + "-" + System.nanoTime()) // PAR 식별 이름 (고유성 보장)
                            .objectName(objectName) // 접근할 파일명
                            .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead) // 읽기 권한
                            .timeExpires(expirationDate) // 만료 시간
                            .build();

            // 2. PAR 생성 요청 객체 생성
            CreatePreauthenticatedRequestRequest request =
                    CreatePreauthenticatedRequestRequest.builder()
                            .namespaceName(namespaceName)
                            .bucketName(bucketName)
                            .createPreauthenticatedRequestDetails(details)
                            .build();

            // 3. OCI에 요청하여 PAR 생성
            CreatePreauthenticatedRequestResponse response =
                    objectStorage.createPreauthenticatedRequest(request);

            // 4. 반환된 URI로 전체 URL 생성
            String accessUri = response.getPreauthenticatedRequest().getAccessUri();
            String fullUrl = String.format("https://%s.objectstorage.%s.oci.customer-oci.com%s",
                    namespaceName, region, accessUri);

            log.info("Presigned URL 생성 완료: objectName={}, expiresInHours={}", objectName, presignedUrlExpirationHours);
            meterRegistry.counter("oracle.objectstorage.url.signing.success").increment();

            return fullUrl;
        });
    }
}
