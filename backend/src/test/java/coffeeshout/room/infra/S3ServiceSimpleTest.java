package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.global.config.properties.QrProperties;
import coffeeshout.global.config.properties.QrProperties.PresignedUrl;
import coffeeshout.global.config.properties.S3Properties;
import coffeeshout.global.exception.custom.StorageServiceException;
import coffeeshout.room.domain.RoomErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ServiceSimpleTest {

    @Mock
    S3Client s3Client;

    @Mock
    S3Presigner s3Presigner;

    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    S3Service s3Service;

    @BeforeEach
    void setUp() {
        int presignedUrlExpirationHours = 24;
        QrProperties qrProperties = new QrProperties(null, 150, 150,
                new PresignedUrl(presignedUrlExpirationHours), "");
        S3Properties s3Properties = new S3Properties("test-bucket");
        s3Service = new S3Service(s3Client, s3Presigner, s3Properties, qrProperties, meterRegistry);
    }

    @Test
    @DisplayName("S3에 파일 업로드가 성공하면 S3 키를 반환한다")
    void upload_Success() {
        // given
        String contents = "TEST123";
        byte[] data = "test image data".getBytes();
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);

        // when
        String result = s3Service.upload(contents, data);

        // then
        String expectedKey = "/TEST123.png";


        assertThat(result).hasToString(expectedKey);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Presigned URL을 성공적으로 생성한다")
    void getUrl_Success() throws Exception {
        // given
        String storageKey = "qr-code/TEST123/uuid.png";
        URL expectedUrl = URI.create("https://test-bucket.s3.amazonaws.com/qr-code/TEST123/uuid.png?signed=true")
                .toURL();

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // when
        String result = s3Service.getUrl(storageKey);

        // then
        assertThat(result).isEqualTo(expectedUrl.toString());

        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("S3 업로드 실패 시 QR_CODE_UPLOAD_FAILED 에러를 던진다")
    void upload_ThrowsException_WhenS3UploadFails() {
        // given
        String contents = "TEST123";
        byte[] data = "test image data".getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException(RoomErrorCode.QR_CODE_UPLOAD_FAILED.getMessage()));

        // when & then
        assertThatThrownBy(() -> s3Service.upload(contents, data))
                .isInstanceOf(StorageServiceException.class);
    }

    @Test
    @DisplayName("Presigned URL 생성 실패 시 QR_CODE_URL_SIGNING_FAILED 에러를 던진다")
    void getUrl_ThrowsException_WhenPresignedUrlFails() {
        // given
        String storageKey = "qr-code/TEST123/uuid.png";

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("Presigned URL generation failed"));

        // when & then
        assertThatThrownBy(() -> s3Service.getUrl(storageKey))
                .isInstanceOf(StorageServiceException.class);
    }

    @Test
    @DisplayName("업로드와 URL 생성을 연속으로 호출하면 정상 동작한다")
    void uploadAndGetUrl_WorksTogether() throws Exception {
        // given
        String contents = "TEST123";
        byte[] data = "test image data".getBytes();
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
        URL expectedUrl = URI.create("https://test-bucket.s3.amazonaws.com/qr-code/TEST123/uuid.png?signed=true")
                .toURL();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // when
        String storageKey = s3Service.upload(contents, data);
        String result = s3Service.getUrl(storageKey);

        // then
        assertThat(result).isEqualTo(expectedUrl.toString());

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }
}
