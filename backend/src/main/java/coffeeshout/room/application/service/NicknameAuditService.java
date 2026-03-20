package coffeeshout.room.application.service;

import coffeeshout.room.config.NicknameAuditProperties;
import coffeeshout.room.domain.audit.NicknameAuditResult;
import coffeeshout.room.domain.audit.NicknameAuditor;
import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.infra.persistence.NicknameAuditEntity;
import coffeeshout.room.infra.persistence.NicknameAuditJpaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameAuditService {

    private final NicknameAuditJpaRepository auditRepository;
    private final NicknameAuditor nicknameAuditor;
    private final NicknameAuditProperties properties;
    private final MeterRegistry meterRegistry;

    private final AtomicLong unauditedQueueDepth = new AtomicLong(0);
    private Counter batchSkippedCounter;

    @PostConstruct
    void initMetrics() {
        Gauge.builder("nickname.audit.unaudited.queue", unauditedQueueDepth, AtomicLong::get)
                .description("스케줄러 실행 시점의 UNAUDITED 닉네임 적체량")
                .register(meterRegistry);
        batchSkippedCounter = Counter.builder("nickname.audit.batch.skipped")
                .description("파싱 실패로 skip된 배치 수")
                .register(meterRegistry);
    }

    public void register(String nickname) {
        auditRepository.save(new NicknameAuditEntity(nickname));
    }

    public void auditPending() {
        long initialQueueSize = auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED);
        unauditedQueueDepth.set(initialQueueSize);
        log.info("닉네임 검열 시작: UNAUDITED 적체량 {}건", initialQueueSize);

        int processedTotal = 0;
        boolean isFirst = true;

        while (true) {
            List<NicknameAuditEntity> batch = auditRepository.findByStatusAndAuditedAtIsNull(
                    NicknameAuditStatus.UNAUDITED,
                    PageRequest.of(0, properties.batchSize(), Sort.by("createdAt").ascending())
            );

            if (batch.isEmpty()) {
                break;
            }

            if (!isFirst) {
                sleepForRateLimit();
            }
            isFirst = false;

            int updated = processBatch(batch);
            processedTotal += updated;

            log.info("닉네임 검열 진행: 이번 배치 {}건, 누적 {}건", batch.size(), processedTotal);

            if (batch.size() < properties.batchSize()) {
                break;
            }
        }

        log.info("닉네임 검열 완료: 총 {}건 처리", processedTotal);
    }

    @Transactional
    protected int processBatch(List<NicknameAuditEntity> batch) {
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
                meterRegistry.counter("nickname.audit.result",
                        "status", result.status().name()).increment();
            }
        });

        auditRepository.saveAll(batch);
        return batch.size();
    }

    private void sleepForRateLimit() {
        try {
            Thread.sleep(properties.batchDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("배치 대기 중 인터럽트 발생");
        }
    }
}
