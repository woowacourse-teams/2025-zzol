package coffeeshout.profanity.application;

import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

    private final NicknameAuditRepository auditRepository;
    private final NicknameAuditor nicknameAuditor;
    private final ProfanityWordManagementService profanityWordManagementService;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;
    private final TextNormalizer textNormalizer;
    private final NicknameAuditProperties nicknameAuditProperties;

    private Counter batchSkippedCounter;

    @PostConstruct
    void initMetrics() {
        batchSkippedCounter = Counter.builder("nickname.audit.batch.skipped")
                .description("파싱 실패로 skip된 배치 수")
                .register(meterRegistry);
    }

    public int process(List<NicknameAudit> batch) {
        final List<String> nicknames =
                batch.stream().map(NicknameAudit::getNickname).distinct().toList();

        final List<NicknameAuditResult> results;
        try {
            results = nicknameAuditor.audit(nicknames);
        } catch (RuntimeException e) {
            // 파싱 실패·빈 응답은 InfrastructureException이고 resilience4j ignore 목록이라
            // (resilience4j.yml의 geminiAudit.ignore-exceptions) 재시도 없이 여기까지 올라온다.
            // 잡지 않으면 배치 하나가 회차를 끝내고 남은 적체가 통째로 다음 회차까지 밀린다.
            batchSkippedCounter.increment();
            log.warn("배치 검열 호출 실패로 {}건 skip — 회차는 다음 배치로 넘어간다", batch.size(), e);
            recordFailure(batch);
            return 0;
        }

        if (results.isEmpty()) {
            batchSkippedCounter.increment();
            log.warn("배치 파싱 실패로 {}건 skip — 다음 스케줄러 실행 시 재시도", batch.size());
            return 0;
        }

        final Map<String, NicknameAuditResult> resultMap = results.stream()
                .collect(Collectors.toMap(NicknameAuditResult::nickname, Function.identity(), (a, b) -> a));

        try {
            final Integer settled = transactionTemplate.execute(status -> settle(batch, nicknames, resultMap));
            return settled == null ? 0 : settled;
        } catch (RuntimeException e) {
            log.warn("배치 저장 실패 {}건 — 같은 판정으로 건별 저장을 다시 시도한다", batch.size(), e);
            return settleIndividually(batch, resultMap);
        }
    }

    /**
     * 벌크 저장이 실패한 배치를 한 건씩 다시 저장한다. 이미 받아둔 판정을 그대로 쓰므로 AI를 다시 부르지 않는다.
     *
     * <p>실패한 트랜잭션은 롤백 상태라 같은 트랜잭션에서 이어 쓸 수 없다. 행마다 새 트랜잭션을 열어,
     * 한 행이 유니크 제약에 걸려도 나머지 판정은 살아남게 한다.
     */
    private int settleIndividually(List<NicknameAudit> batch, Map<String, NicknameAuditResult> resultMap) {
        int settled = 0;
        for (final NicknameAudit entity : batch) {
            try {
                final Integer one = transactionTemplate.execute(
                        status -> settle(List.of(entity), List.of(entity.getNickname()), resultMap));
                settled += one == null ? 0 : one;
            } catch (RuntimeException e) {
                log.warn("건별 저장도 실패 — UNAUDITED로 남긴다. nickname={}", entity.getNickname(), e);
            }
        }
        return settled;
    }

    /**
     * 실패한 배치 행들의 시도 횟수를 올린다. 상한에 닿은 행은 리포지토리가 DEAD_LETTER로 내려
     * 다음 회차의 UNAUDITED 스캔에서 뺀다.
     */
    private void recordFailure(List<NicknameAudit> batch) {
        final List<Long> ids = batch.stream().map(NicknameAudit::getId).toList();
        final int maxAttempts = nicknameAuditProperties.maxAttempts();
        final Integer deadLettered = transactionTemplate.execute(status -> {
            auditRepository.incrementAttemptCount(ids);
            return auditRepository.markDeadLetterAtAttemptLimit(ids, maxAttempts);
        });
        log.warn("검열 실패 {}건 기록 — 시도 {}회 상한에 닿아 DEAD_LETTER로 내린 행 {}", batch.size(), maxAttempts, deadLettered);
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
        meterRegistry
                .counter("nickname.audit.result", "status", result.status().name())
                .increment();
        if (result.status() == NicknameAuditStatus.FLAGGED) {
            autoBlock(result);
        }
    }

    private void autoBlock(NicknameAuditResult result) {
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
