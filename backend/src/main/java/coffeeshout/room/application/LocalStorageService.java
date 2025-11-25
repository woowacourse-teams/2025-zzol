package coffeeshout.room.application;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.domain.RoomErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"local", "test"})
@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final MeterRegistry meterRegistry;

    public LocalStorageService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String upload(String contents, byte[] data) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(data);
            log.info("로컬 환경에서 데이터 업로드 완료: contents={}, dataSize={}", contents, data.length);
            meterRegistry.counter("qr.local.upload.success").increment();
            
            // 로컬에서는 base64 인코딩된 데이터를 저장 키로 사용
            return base64Image;
        } catch (Exception e) {
            meterRegistry.counter("qr.local.upload.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("로컬 데이터 업로드 실패: contents={}, error={}", contents, e.getMessage(), e);
            throw new InvalidArgumentException(RoomErrorCode.QR_CODE_UPLOAD_FAILED, "로컬 QR 코드 업로드에 실패했습니다.");
        }
    }
    
    @Override
    public String getUrl(String storageKey) {
        try {
            String dataUrl = "data:image/png;base64," + storageKey;
            log.info("로컬 환경에서 Data URL 생성 완료: length={}", dataUrl.length());
            meterRegistry.counter("qr.local.url.generation.success").increment();
            
            return dataUrl;
        } catch (Exception e) {
            meterRegistry.counter("qr.local.url.generation.failed",
                    "error", e.getClass().getSimpleName()).increment();
            log.error("로컬 Data URL 생성 실패: error={}", e.getMessage(), e);
            throw new InvalidArgumentException(RoomErrorCode.QR_CODE_URL_SIGNING_FAILED, "로컬 QR 코드 URL 생성에 실패했습니다.");
        }
    }
}
