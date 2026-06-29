package coffeeshout.profanity.application;

import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
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
        final List<String> nicknames = batch.stream()
                .map(NicknameAudit::getNickname)
                .distinct()
                .toList();

        final List<NicknameAuditResult> results = nicknameAuditor.audit(nicknames);

        if (results.isEmpty()) {
            batchSkippedCounter.increment();
            log.warn("배치 파싱 실패로 {}건 skip — 다음 스케줄러 실행 시 재시도", batch.size());
            return 0;
        }

        final Map<String, NicknameAuditResult> resultMap = results.stream()
                .collect(Collectors.toMap(NicknameAuditResult::nickname, Function.identity(), (a, b) -> a));

        transactionTemplate.executeWithoutResult(status -> {
            batch.forEach(entity -> applyResult(entity, resultMap.get(entity.getNickname())));
            auditRepository.saveAll(batch);
        });

        return batch.size();
    }

    private void applyResult(NicknameAudit entity, NicknameAuditResult result) {
        if (result == null) return;
        entity.complete(result.status(), result.confidence(), result.reason());
        meterRegistry.counter("nickname.audit.result", "status", result.status().name()).increment();
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
            log.info("유효한 비속어 조각 없음 — 닉네임 전체 차단 폴백: nickname={}, terms={}",
                    result.nickname(), result.profanityTerms());
            return List.of(result.nickname());
        }
        return fragments;
    }
}
