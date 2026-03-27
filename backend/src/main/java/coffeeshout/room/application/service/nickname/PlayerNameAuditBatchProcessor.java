package coffeeshout.room.application.service.nickname;

import coffeeshout.room.domain.audit.PlayerNameAuditResult;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.domain.audit.PlayerNameAuditor;
import coffeeshout.room.infra.event.ProfanityWordBlockedEvent;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityEntity.Source;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityJpaRepository;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditJpaRepository;
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
public class PlayerNameAuditBatchProcessor {

    private final PlayerNameAuditJpaRepository auditRepository;
    private final PlayerNameAuditor playerNameAuditor;
    private final CustomProfanityJpaRepository customProfanityRepository;
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

    public int process(List<PlayerNameAuditEntity> batch) {
        final List<String> playerNames = batch.stream()
                .map(PlayerNameAuditEntity::getPlayerName)
                .distinct()
                .toList();

        final List<PlayerNameAuditResult> results = playerNameAuditor.audit(playerNames);

        if (results.isEmpty()) {
            batchSkippedCounter.increment();
            log.warn("배치 파싱 실패로 {}건 skip — 다음 스케줄러 실행 시 재시도", batch.size());
            return 0;
        }

        final Map<String, PlayerNameAuditResult> resultMap = results.stream()
                .collect(Collectors.toMap(PlayerNameAuditResult::playerName, Function.identity(), (a, b) -> a));

        transactionTemplate.executeWithoutResult(status -> {
            batch.forEach(entity -> applyResult(entity, resultMap.get(entity.getPlayerName())));
            auditRepository.saveAll(batch);
        });

        return batch.size();
    }

    private void applyResult(PlayerNameAuditEntity entity, PlayerNameAuditResult result) {
        if (result == null) return;
        entity.complete(result.status(), result.confidence(), result.reason());
        meterRegistry.counter("nickname.audit.result", "status", result.status().name()).increment();
        if (result.status() == PlayerNameAuditStatus.FLAGGED) {
            autoBlock(entity.getPlayerName());
        }
    }

    private void autoBlock(String playerName) {
        final int inserted = customProfanityRepository.insertIgnore(playerName, Source.AI_AUDIT.name());
        if (inserted == 0) return;
        eventPublisher.publishEvent(new ProfanityWordBlockedEvent(playerName));
        log.info("FLAGGED 자동 차단: {}", playerName);
    }
}
