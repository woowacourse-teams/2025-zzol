package coffeeshout.profanity.fixture;

import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.config.NicknameAuditProperties.Seed;
import coffeeshout.profanity.config.NicknameAuditProperties.Stub;
import java.time.Duration;

/**
 * {@link NicknameAuditProperties}는 생성자 바인딩 record라 필드를 하나 늘릴 때마다 모든 호출부가 깨진다.
 * 조립을 여기 모아 테스트가 자기가 검증할 값만 넘기게 한다.
 */
public final class NicknameAuditPropertiesFixture {

    private static final String 모델 = "gemini-3.5-flash";
    private static final int 배치_크기 = 100;
    private static final Duration 요청_타임아웃_기본 = Duration.ofSeconds(120);
    private static final Duration 회차_예산_기본 = Duration.ofMinutes(10);
    private static final int 시도_상한_기본 = 3;
    private static final Stub 스텁_꺼짐 = new Stub(Duration.ZERO, 0);

    private NicknameAuditPropertiesFixture() {}

    public static NicknameAuditProperties API_키(String geminiApiKey) {
        return of(geminiApiKey, 배치_크기, 요청_타임아웃_기본, 회차_예산_기본, 시도_상한_기본, 스텁_꺼짐, new Seed(0));
    }

    public static NicknameAuditProperties 요청_타임아웃(Duration requestTimeout) {
        return of("api-key", 배치_크기, requestTimeout, 회차_예산_기본, 시도_상한_기본, 스텁_꺼짐, new Seed(0));
    }

    public static NicknameAuditProperties 회차(int batchSize, Duration maxRunDuration, int maxAttempts) {
        return of("api-key", batchSize, 요청_타임아웃_기본, maxRunDuration, maxAttempts, 스텁_꺼짐, new Seed(0));
    }

    public static NicknameAuditProperties 스텁(Duration latency, double flaggedRatio) {
        return of("api-key", 배치_크기, 요청_타임아웃_기본, 회차_예산_기본, 시도_상한_기본, new Stub(latency, flaggedRatio), new Seed(0));
    }

    public static NicknameAuditProperties 적재(int seedCount) {
        return of("api-key", 배치_크기, 요청_타임아웃_기본, 회차_예산_기본, 시도_상한_기본, 스텁_꺼짐, new Seed(seedCount));
    }

    private static NicknameAuditProperties of(
            String geminiApiKey,
            int batchSize,
            Duration requestTimeout,
            Duration maxRunDuration,
            int maxAttempts,
            Stub stub,
            Seed seed) {
        return new NicknameAuditProperties(
                geminiApiKey,
                모델,
                0.85,
                batchSize,
                20,
                2,
                requestTimeout,
                maxRunDuration,
                maxAttempts,
                "0 0 0/12 * * *",
                seed,
                stub);
    }
}
