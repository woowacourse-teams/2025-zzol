package coffeeshout.room.infra;

import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.config.properties.S3Properties;
import coffeeshout.global.exception.custom.StorageServiceException;
import coffeeshout.room.application.StorageService;
import coffeeshout.room.domain.RoomErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.net.URL;
import java.time.Duration;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Service
@Profile("!local & !test")
public class S3Service implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final int presignedUrlExpirationHours;
    private final MeterRegistry meterRegistry;
    private final Timer s3UploadTimer;
    private final Timer s3PresignerTimer;
    private final String s3KeyPrefix;

    public S3Service(S3Client s3Client,
                     S3Presigner s3Presigner,
                     S3Properties s3Properties,
                     QrProperties qrProperties,
                     MeterRegistry meterRegistry
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = s3Properties.bucket();
        this.presignedUrlExpirationHours = qrProperties.presignedUrl().expirationHours();
        this.meterRegistry = meterRegistry;
        this.s3UploadTimer = Timer.builder("s3.upload.time")
                .description("Time taken to upload QR code to S3")
                .register(meterRegistry);
        this.s3PresignerTimer = Timer.builder("s3.presigner.upload.timer").register(meterRegistry);
        this.s3KeyPrefix = qrProperties.s3KeyPrefix();
    }

    @Override
    public String upload(@NonNull String contents, byte[] data) {
        if (data.length == 0) {
            throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED, "QR 이미지 바이트가 비어 있습니다.");
        }

        try {
            return uploadQrCodeToS3(contents, data);
        } catch (Exception e) {
            meterRegistry.counter("s3.qr.upload.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("S3 QR 코드 업로드 실패: contents={}, error={}", contents, e.getMessage(), e);
            throw new StorageServiceException(RoomErrorCode.QR_CODE_UPLOAD_FAILED, RoomErrorCode.QR_CODE_UPLOAD_FAILED.getMessage(), e);
        }
    }

    @Override
    public String getUrl(String storageKey) {
        try {
            return generatePresignedUrl(storageKey);
        } catch (Exception e) {
            meterRegistry.counter("s3.qr.url.signing.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("S3 Presigned URL 생성 실패: storageKey={}, error={}", storageKey, e.getMessage(), e);
            throw new StorageServiceException(RoomErrorCode.QR_CODE_URL_SIGNING_FAILED,
                    RoomErrorCode.QR_CODE_URL_SIGNING_FAILED.getMessage());
        }
    }

    private String uploadQrCodeToS3(String contents, byte[] qrCodeImage) throws Exception {
        return s3UploadTimer.recordCallable(() -> {
            String s3Key = s3KeyPrefix + "/" + contents + ".png";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("image/png")
                    .contentLength((long) qrCodeImage.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(qrCodeImage));
            log.info("QR 코드 S3 업로드 완료: contents={}, s3Key={}", contents, s3Key);

            meterRegistry.counter("s3.upload.success").increment();
            return s3Key;
        });
    }

    private String generatePresignedUrl(String s3Key) throws Exception {
        return s3PresignerTimer.recordCallable(
                () -> {
                    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofHours(presignedUrlExpirationHours))
                            .getObjectRequest(b -> b
                                    .bucket(bucketName)
                                    .key(s3Key)
                            )
                            .build();

                    URL presignedUrl = s3Presigner.presignGetObject(presignRequest).url();
                    log.info("Presigned URL 생성 완료: s3Key={}, expiresInHours={}", s3Key, presignedUrlExpirationHours);
                    meterRegistry.counter("s3.url.signing.success").increment();

                    return presignedUrl.toString();
                }
        );
    }
}
