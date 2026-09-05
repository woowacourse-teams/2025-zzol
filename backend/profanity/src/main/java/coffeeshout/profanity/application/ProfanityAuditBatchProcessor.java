package coffeeshout.profanity.application;

import coffeeshout.global.exception.ErrorCode;
import coffeeshout.global.exception.custom.CoffeeShoutException;
import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityAuditBatchProcessor {

    /**
     * 회차 구간별 소요 시간. phase 태그로 나눈다. LLM 없이 회차를 돌려 어디가 느린지 볼 때 이게 근거가 된다.
     *
     * <p>{@code settle}은 {@code block}을 안에 포함한다. 자동 차단이 승격 저장 트랜잭션 안에서 돌기 때문이다.
     * 합이 회차 전체 시간과 맞아떨어지지 않는 건 그래서다.
     */
    public static final String PHASE_TIMER = "nickname.audit.phase";

    static final String PHASE_TIMER_DESCRIPTION = "닉네임 검열 회차의 구간별 소요 시간 (settle은 block을 포함한다)";

    /** 배치 내용이 원인이라 다시 불러도 같은 결과가 나오는 실패. 이것만 시도 횟수에 센다. */
    private static final Set<ErrorCode> DETERMINISTIC_AUDIT_FAILURES = Set.of(
            NicknameAuditErrorCode.AI_RESPONSE_PARSE_FAILED,
            NicknameAuditErrorCode.AI_EMPTY_RESPONSE,
            NicknameAuditErrorCode.PROMPT_BUILD_FAILED);

    private final NicknameAuditRepository auditRepository;
    private final NicknameAuditor nicknameAuditor;
    private final ProfanityWordManagementService profanityWordManagementService;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;
    private final TextNormalizer textNormalizer;
    private final NicknameAuditProperties nicknameAuditProperties;

    private Counter batchSkippedCounter;
    private Counter deadLetteredCounter;
    private Timer auditPhaseTimer;
    private Timer settlePhaseTimer;
    private Timer blockPhaseTimer;

    @PostConstruct
    void initMetrics() {
        batchSkippedCounter = Counter.builder("nickname.audit.batch.skipped")
                .description("검열 호출 실패로 skip된 배치 수")
                .register(meterRegistry);
        deadLetteredCounter = Counter.builder("nickname.audit.dead.lettered")
                .description("시도 횟수 상한에 닿아 DEAD_LETTER로 내려간 행 수")
                .register(meterRegistry);
        auditPhaseTimer = phaseTimer("audit");
        settlePhaseTimer = phaseTimer("settle");
        blockPhaseTimer = phaseTimer("block");
    }

    private Timer phaseTimer(String phase) {
        return Timer.builder(PHASE_TIMER)
                .description(PHASE_TIMER_DESCRIPTION)
                .tag("phase", phase)
                .register(meterRegistry);
    }

    public int process(List<NicknameAudit> batch) {
        final List<String> nicknames =
                batch.stream().map(NicknameAudit::getNickname).distinct().toList();

        final List<NicknameAuditResult> results;
        try {
            results = auditPhaseTimer.record(() -> nicknameAuditor.audit(nicknames));
        } catch (RuntimeException e) {
            // 파싱 실패·빈 응답은 InfrastructureException이고 resilience4j ignore 목록이라
            // (resilience4j.yml의 geminiAudit.ignore-exceptions) 재시도 없이 여기까지 올라온다.
            // 잡지 않으면 배치 하나가 회차를 끝내고 남은 적체가 통째로 다음 회차까지 밀린다.
            batchSkippedCounter.increment();
            log.warn("배치 검열 호출 실패로 {}건 skip — 회차는 다음 배치로 넘어간다", batch.size(), e);
            if (!isDeterministicFailure(e)) {
                return 0;
            }
            return recordFailure(batch);
        }

        if (results.isEmpty()) {
            batchSkippedCounter.increment();
            log.warn("배치 파싱 실패로 {}건 skip — 다음 스케줄러 실행 시 재시도", batch.size());
            return recordFailure(batch);
        }

        final Map<String, NicknameAuditResult> resultMap = results.stream()
                .collect(Collectors.toMap(NicknameAuditResult::nickname, Function.identity(), (a, b) -> a));

        try {
            final Integer settled = settlePhaseTimer.record(
                    () -> transactionTemplate.execute(status -> settle(batch, nicknames, resultMap)));
            return settled == null ? 0 : settled;
        } catch (RuntimeException e) {
            log.warn("배치 저장 실패 {}건 — 같은 판정으로 건별 저장을 다시 시도한다", batch.size(), e);
            return settlePhaseTimer.record(() -> settleIndividually(batch, resultMap));
        }
    }

    /**
     * 배치 내용 때문에 다시 불러도 똑같이 실패하는 오류인지 본다.
     *
     * <p>일시적 실패까지 세면 멀쩡한 닉네임이 DEAD_LETTER로 내려간다. {@code GeminiNicknameAuditor}가
     * 네트워크 끊김·레이트리밋·타임아웃을 전부 {@code AI_CALL_FAILED}로 감싸는데, 그건 다음 회차에 풀리는 쪽이다.
     */
    private static boolean isDeterministicFailure(RuntimeException e) {
        return e instanceof CoffeeShoutException exception
                && DETERMINISTIC_AUDIT_FAILURES.contains(exception.getErrorCode());
    }

    /**
     * 벌크 저장이 실패한 배치를 한 건씩 다시 저장한다. 이미 받아둔 판정을 그대로 쓰므로 AI를 다시 부르지 않는다.
     *
     * <p>여기서는 {@code settle}을 다시 돌리지 않고 저장만 한다. 벌크 트랜잭션은 커밋에 성공하고도
     * 예외를 낼 수 있다. {@code ProfanityWordBlockedEvent} 수신자가 AFTER_COMMIT + REQUIRES_NEW라
     * 거기서 난 예외가 커밋 뒤에 올라온다. 그 경우 {@code settle}을 다시 돌리면 방금 커밋한 행을
     * 자기 자신의 terminal 트윈으로 착각해 지워버린다. 저장만 하면 같은 값을 다시 쓰는 것이라 안전하다.
     *
     * <p>행마다 새 트랜잭션을 연다. 실패한 트랜잭션은 롤백 상태라 이어 쓸 수 없고, 한 행이 제약에 걸려도
     * 나머지 판정은 살아남아야 한다.
     */
    private int settleIndividually(List<NicknameAudit> batch, Map<String, NicknameAuditResult> resultMap) {
        int settled = 0;
        for (final NicknameAudit entity : batch) {
            final NicknameAuditResult result = resultMap.get(entity.getNickname());
            if (result == null) {
                continue;
            }
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    applyResult(entity, result);
                    auditRepository.save(entity);
                });
                countResults(List.of(entity));
                settled++;
            } catch (RuntimeException e) {
                log.warn("건별 저장도 실패 — UNAUDITED로 남긴다. nickname={}", entity.getNickname(), e);
            }
        }
        return settled;
    }

    /**
     * 실패한 배치 행들의 시도 횟수를 올리고, 상한에 닿아 DEAD_LETTER로 내려간 행 수를 돌려준다.
     *
     * <p>DEAD_LETTER는 UNAUDITED 스캔에서 빠지므로 드레인 루프에는 진행이다. 0을 돌려주면 스캔이
     * 앞으로 당겨진 만큼을 커서가 그냥 건너뛴다.
     *
     * <p>여기서 난 예외는 삼킨다. 이 메서드는 검열 호출 실패를 잡은 자리에서 불리는데, 다시 예외를
     * 올리면 배치 하나의 실패가 회차를 끝내는 것을 막으려던 try/catch가 무의미해진다.
     */
    private int recordFailure(List<NicknameAudit> batch) {
        final List<Long> ids = batch.stream().map(NicknameAudit::getId).toList();
        final int maxAttempts = nicknameAuditProperties.maxAttempts();
        try {
            final Integer deadLettered = transactionTemplate.execute(status -> {
                final int attempted = auditRepository.incrementAttemptCount(ids);
                if (attempted != ids.size()) {
                    // UNAUDITED인 행만 올린다. 차이가 나면 그 사이 다른 인스턴스가 같은 행을 판정했거나
                    // 이미 DEAD_LETTER로 내려간 것이다.
                    log.warn("시도 횟수가 오른 행이 배치보다 적다: 배치 {}행, 갱신 {}행", ids.size(), attempted);
                }
                return auditRepository.markDeadLetterAtAttemptLimit(ids, maxAttempts);
            });
            log.warn("검열 실패 {}건 기록 — 시도 {}회 상한에 닿아 DEAD_LETTER로 내린 행 {}", batch.size(), maxAttempts, deadLettered);
            if (deadLettered == null) {
                return 0;
            }
            deadLetteredCounter.increment(deadLettered);
            return deadLettered;
        } catch (RuntimeException e) {
            log.error("시도 횟수 기록 실패 — 이번 배치는 세지 않고 넘어간다", e);
            return 0;
        }
    }

    /**
     * 판정을 DB에 반영하고 <b>실제로 UNAUDITED에서 빠져나간 행 수</b>를 돌려준다.
     *
     * <p>배치 크기를 그대로 돌려주면 안 된다. 판정을 못 짝지어 그대로 남은 행까지 처리했다고 세면,
     * 드레인 루프가 진행이 없는데도 같은 0페이지를 계속 다시 읽는다.
     */
    private int settle(List<NicknameAudit> batch, List<String> nicknames, Map<String, NicknameAuditResult> resultMap) {
        // 배치 닉네임 중 이미 검열 완료(terminal) 행을 가진 것들을 한 번에 조회한다 (건별 조회 N+1 회피).
        final Set<String> nicknamesWithTerminal = auditRepository.findNicknamesWithTerminalStatus(nicknames);

        final List<NicknameAudit> toPromote = new ArrayList<>();
        final List<NicknameAudit> redundant = new ArrayList<>();
        int unmatched = 0;
        for (final NicknameAudit entity : batch) {
            final NicknameAuditResult result = resultMap.get(entity.getNickname());
            if (result == null) {
                unmatched++;
                continue;
            }
            // 같은 닉네임의 검열 완료(terminal) 행이 이미 있으면 이 UNAUDITED는 #1467 fix 이전에 생긴
            // 잔존 중복 재등록이다(register의 "이름당 1행" 불변식 위반). 승격하면 대상 상태가 같을 때
            // uq_player_name_audit_name_status에 충돌해 배치 전체가 롤백되고 매 tick 재크래시하며,
            // 다를 때는 (예: 기존 CLEAN + 신규 FLAGGED) 모순된 감사 이력과 autoBlock 오발동을 남긴다.
            // 어느 쪽이든 승격 대신 제거한다 — 기존 terminal 행이 권위 있는 판정을 이미 보유한다.
            if (nicknamesWithTerminal.contains(entity.getNickname())) {
                redundant.add(entity);
                continue;
            }
            applyResult(entity, result);
            toPromote.add(entity);
        }
        auditRepository.saveAll(toPromote);
        countResults(toPromote);
        if (!redundant.isEmpty()) {
            auditRepository.deleteAll(redundant);
            log.warn("이미 검열된 닉네임의 잔존 UNAUDITED {}건 제거 (중복 재등록)", redundant.size());
        }
        if (unmatched > 0) {
            log.warn("판정을 짝지을 수 없는 닉네임 {}건 — UNAUDITED로 남긴다", unmatched);
        }
        return toPromote.size() + redundant.size();
    }

    private void applyResult(NicknameAudit entity, NicknameAuditResult result) {
        entity.complete(result.status(), result.confidence(), result.reason());
        if (result.status() == NicknameAuditStatus.FLAGGED) {
            autoBlock(result);
        }
    }

    /**
     * 저장에 성공한 뒤에만 부른다. 판정을 반영하는 자리에서 올리면 벌크 저장이 실패해 폴백이 도는 경로에서
     * 같은 행이 두 번 세어진다. 카운터는 롤백되지 않는다.
     */
    private void countResults(List<NicknameAudit> saved) {
        saved.forEach(entity -> meterRegistry
                .counter("nickname.audit.result", "status", entity.getStatus().name())
                .increment());
    }

    private void autoBlock(NicknameAuditResult result) {
        blockPhaseTimer.record(() -> blockWords(result));
    }

    private void blockWords(NicknameAuditResult result) {
        resolveBlockWords(result).forEach(word -> {
            final Language language = Language.detect(textNormalizer.normalize(word));
            if (profanityWordManagementService.add(word, language, WordSource.AI_FLAGGED)) {
                eventPublisher.publishEvent(new ProfanityWordBlockedEvent(word));
                log.info("FLAGGED 자동 차단: nickname={}, word={}", result.nickname(), word);
            }
        });
    }

    /**
     * 도메인이 골라낸 유효 비속어 조각을 차단 대상으로 채택한다.
     * 유효한 조각이 하나도 없으면 닉네임 전체를 차단 대상으로 폴백한다(폴백 정책은 application의 책임).
     */
    private List<String> resolveBlockWords(NicknameAuditResult result) {
        final List<String> fragments =
                result.extractProfanityFragments(textNormalizer, nicknameAuditProperties.minTermLength());
        if (fragments.isEmpty()) {
            log.info("유효한 비속어 조각 없음 — 닉네임 전체 차단 폴백: nickname={}, terms={}", result.nickname(), result.profanityTerms());
            return List.of(result.nickname());
        }
        return fragments;
    }
}
