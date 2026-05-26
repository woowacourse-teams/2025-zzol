package coffeeshout.profanity.application;

import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.Language;
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
            autoBlock(entity.getNickname());
        }
    }

    private void autoBlock(String nickname) {
        if (profanityWordManagementService.add(nickname, Language.detect(nickname), WordSource.AI_FLAGGED)) {
            eventPublisher.publishEvent(new ProfanityWordBlockedEvent(nickname));
            log.info("FLAGGED 자동 차단: {}", nickname);
        }
    }
}
