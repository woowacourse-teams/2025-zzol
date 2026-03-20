package coffeeshout.room.application.service.nickname;

import coffeeshout.room.domain.audit.NicknameAuditResult;
import coffeeshout.room.domain.audit.NicknameAuditor;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditJpaRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameAuditBatchProcessor {

    private final NicknameAuditJpaRepository auditRepository;
    private final NicknameAuditor nicknameAuditor;
    private final MeterRegistry meterRegistry;

    private Counter batchSkippedCounter;

    @PostConstruct
    void initMetrics() {
        batchSkippedCounter = Counter.builder("nickname.audit.batch.skipped")
                .description("파싱 실패로 skip된 배치 수")
                .register(meterRegistry);
    }

    @RateLimiter(name = "geminiAudit")
    @Transactional
    public int process(List<NicknameAuditEntity> batch) {
        List<String> nicknames = batch.stream()
                .map(NicknameAuditEntity::getNickname)
                .distinct()
                .toList();

        List<NicknameAuditResult> results = nicknameAuditor.audit(nicknames);

        if (results.isEmpty()) {
            batchSkippedCounter.increment();
            log.warn("배치 파싱 실패로 {}건 skip — 다음 스케줄러 실행 시 재시도", batch.size());
            return 0;
        }

        Map<String, NicknameAuditResult> resultMap = results.stream()
                .collect(Collectors.toMap(NicknameAuditResult::nickname, Function.identity(), (a, b) -> a));

        batch.forEach(entity -> {
            NicknameAuditResult result = resultMap.get(entity.getNickname());
            if (result != null) {
                entity.complete(result.status(), result.confidence(), result.reason());
                meterRegistry.counter("nickname.audit.result", "status", result.status().name()).increment();
            }
        });

        auditRepository.saveAll(batch);
        return batch.size();
    }
}
