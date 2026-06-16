package coffeeshout.profanity.application;

import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
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
import java.util.LinkedHashMap;
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

    private static final int MIN_TERM_LENGTH = 2;

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
            autoBlock(entity.getNickname(), result.profanityTerms());
        }
    }

    private void autoBlock(String nickname, List<String> profanityTerms) {
        resolveBlockWords(nickname, profanityTerms).forEach(word -> {
            if (profanityWordManagementService.add(word, Language.detect(word), WordSource.AI_FLAGGED)) {
                eventPublisher.publishEvent(new ProfanityWordBlockedEvent(word));
                log.info("FLAGGED 자동 차단: nickname={}, word={}", nickname, word);
            }
        });
    }

    /**
     * AI가 추출한 비속어 조각 중 원본 닉네임의 (정규화 기준) 부분 문자열인 것만 차단 대상으로 채택한다.
     * 정규화 후 길이가 너무 짧은 조각은 Trie 부분일치 특성상 과차단을 유발하므로 제외하고,
     * 정규화 결과가 같은 중복 조각은 한 번만 등록한다.
     * 유효한 조각이 하나도 없으면 닉네임 전체를 차단 대상으로 폴백한다.
     */
    private List<String> resolveBlockWords(String nickname, List<String> profanityTerms) {
        final String normalizedNickname = textNormalizer.normalize(nickname);
        final Map<String, String> validByNormalized = new LinkedHashMap<>();
        for (String term : profanityTerms) {
            if (term == null || term.isBlank()) {
                continue;
            }
            final String normalizedTerm = textNormalizer.normalize(term);
            if (normalizedTerm.length() < MIN_TERM_LENGTH || !normalizedNickname.contains(normalizedTerm)) {
                continue;
            }
            validByNormalized.putIfAbsent(normalizedTerm, term);
        }
        if (validByNormalized.isEmpty()) {
            log.info("유효한 비속어 조각 없음 — 닉네임 전체 차단 폴백: nickname={}, terms={}", nickname, profanityTerms);
            return List.of(nickname);
        }
        return List.copyOf(validByNormalized.values());
    }
}
