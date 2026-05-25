package coffeeshout.profanity.application;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditEntity;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameAuditService {

    private final NicknameAuditRepository auditRepository;
    private final NicknameAuditBatchProcessor batchProcessor;
    private final NicknameAuditProperties properties;
    private final MeterRegistry meterRegistry;

    private final AtomicLong unauditedQueueDepth = new AtomicLong(0);

    @PostConstruct
    void initMetrics() {
        Gauge.builder("nickname.audit.unaudited.queue", unauditedQueueDepth, AtomicLong::get)
                .description("스케줄러 실행 시점의 UNAUDITED 닉네임 적체량")
                .register(meterRegistry);
    }

    public Page<NicknameAuditEntity> listByStatus(NicknameAuditStatus status, Pageable pageable) {
        return auditRepository.findByStatus(status, pageable);
    }

    public void register(String nickname) {
        if (auditRepository.existsByNicknameAndStatus(nickname, NicknameAuditStatus.UNAUDITED)) {
            log.debug("이미 UNAUDITED 상태로 등록된 닉네임: {}", nickname);
            return;
        }
        auditRepository.save(new NicknameAuditEntity(nickname));
    }

    public void auditPending() {
        final long initialQueueSize = auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED);
        unauditedQueueDepth.set(initialQueueSize);
        log.info("닉네임 검열 시작: UNAUDITED 적체량 {}건", initialQueueSize);

        final Pageable pageable = PageRequest.of(0, properties.batchSize(), Sort.by("createdAt").ascending());
        List<NicknameAuditEntity> batch = auditRepository.findByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED, pageable);
        int processedTotal = 0;

        while (!batch.isEmpty()) {
            processedTotal += batchProcessor.process(batch);
            log.info("닉네임 검열 진행: 이번 배치 {}건, 누적 {}건", batch.size(), processedTotal);

            if (batch.size() < properties.batchSize()) {
                break;
            }
            batch = auditRepository.findByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED, pageable);
        }

        log.info("닉네임 검열 완료: 총 {}건 처리", processedTotal);
    }
}
