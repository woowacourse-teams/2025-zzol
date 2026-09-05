package coffeeshout.profanity.infra;

import static java.util.Objects.requireNonNull;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local·test 프로파일에서 Gemini 대신 도는 스텁 검열기.
 *
 * <p>{@code nickname-audit.stub}으로 지연과 FLAGGED 비율을 주면 LLM 없이 파이프라인 내부 병목을 잴 수 있다.
 * 지연 0에 FLAGGED 비율을 0보다 크게 두면 자동 차단과 트라이 재빌드까지 실제로 타면서 LLM 시간만 빠진다.
 *
 * <p>판정은 닉네임 해시로 정해 같은 입력에 같은 FLAGGED 집합이 나온다. 회차를 여러 번 돌려 비교하려면
 * 결정론이 필요하다.
 */
@Slf4j
@Component
@Profile("local | test")
public class NoOpNicknameAuditor implements NicknameAuditor {

    /** 차단 조각으로 쓸 한글 구간. 정규화한 닉네임의 부분 문자열이어야 도메인이 조각으로 채택한다. */
    private static final Pattern HANGUL_RUN = Pattern.compile("[가-힣]{2,}");

    private static final int RATIO_BUCKETS = 100;

    private final Duration latency;
    private final double flaggedRatio;

    public NoOpNicknameAuditor(NicknameAuditProperties properties) {
        this.latency = properties.stub().latency();
        this.flaggedRatio = properties.stub().flaggedRatio();
    }

    @Override
    public List<NicknameAuditResult> audit(List<String> nicknames) {
        requireNonNull(nicknames, "nicknames은 null일 수 없습니다.");
        log.debug("NoOpNicknameAuditor: Gemini 호출 생략 (local/test 프로파일), nicknames={}", nicknames);

        sleepStubLatency();
        return nicknames.stream().map(this::judge).toList();
    }

    private NicknameAuditResult judge(String nickname) {
        if (!isFlagged(nickname)) {
            return new NicknameAuditResult(nickname, NicknameAuditStatus.CLEAN, AiConfidence.UNKNOWN, "no-op");
        }
        return new NicknameAuditResult(
                nickname, NicknameAuditStatus.FLAGGED, AiConfidence.of(1.0), "no-op stub", blockTerms(nickname));
    }

    private boolean isFlagged(String nickname) {
        if (flaggedRatio <= 0) {
            return false;
        }
        return Math.floorMod(nickname.hashCode(), RATIO_BUCKETS) < Math.round(flaggedRatio * RATIO_BUCKETS);
    }

    /**
     * 닉네임에서 한글 두 글자를 잘라 차단 조각으로 쓴다. 조각을 비워두면 자동 차단이 닉네임 전체를 사전에 넣어
     * 측정하려는 경로가 실제와 달라진다. 자르는 위치도 해시로 정해 닉네임마다 다른 단어가 사전에 들어간다.
     */
    private List<String> blockTerms(String nickname) {
        final Matcher matcher = HANGUL_RUN.matcher(nickname);
        if (!matcher.find()) {
            return List.of();
        }
        final String run = matcher.group();
        final int start = Math.floorMod(nickname.hashCode(), run.length() - 1);
        return List.of(run.substring(start, start + 2));
    }

    private void sleepStubLatency() {
        if (latency.isZero() || latency.isNegative()) {
            return;
        }
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            // 실행기의 shutdownNow가 보낸 인터럽트다. 플래그를 되살려야 드레인 루프가 회차를 멈춘다.
            Thread.currentThread().interrupt();
            throw new InfrastructureException(NicknameAuditErrorCode.AI_CALL_FAILED, "스텁 지연 대기가 중단됐습니다.", e);
        }
    }
}
