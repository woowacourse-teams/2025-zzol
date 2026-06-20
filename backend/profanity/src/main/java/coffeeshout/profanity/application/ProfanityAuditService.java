package coffeeshout.profanity.application;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.global.nickname.NicknameSubmittedEvent;
import coffeeshout.profanity.domain.audit.NicknameAudit;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityAuditService {

    private final NicknameAuditRepository auditRepository;
    private final ProfanityAuditBatchProcessor batchProcessor;
    private final ProfanityWordManagementService profanityWordManagementService;
    private final NicknameAuditProperties properties;
    private final MeterRegistry meterRegistry;

    private final AtomicLong unauditedQueueDepth = new AtomicLong(0);

    @PostConstruct
    void initMetrics() {
        Gauge.builder("nickname.audit.unaudited.queue", unauditedQueueDepth, AtomicLong::get)
                .description("스케줄러 실행 시점의 UNAUDITED 닉네임 적체량")
                .register(meterRegistry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNicknameSubmitted(NicknameSubmittedEvent event) {
        log.debug("닉네임 검열 등록 요청 수신: {}", event.nickname());
        register(event.nickname());
    }

    public Page<NicknameAudit> listByStatus(NicknameAuditStatus status, Pageable pageable) {
        return auditRepository.findByStatus(status, pageable);
    }

    public void register(String nickname) {
        if (profanityWordManagementService.isOperatorAllowed(nickname)) {
            log.debug("운영자 허용 닉네임 — 검열 등록 생략: {}", nickname);
            return;
        }
        if (auditRepository.existsByNickname(nickname)) {
            // 상태 무관으로 검사한다. 유니크 제약이 (player_name, status)라 이미 검열된(CLEAN 등)
            // 닉네임에 대해 UNAUDITED만 검사하면 새 UNAUDITED 중복이 생기고, 다음 검열 시
            // 기존 상태로 승격되며 유니크 충돌이 발생한다 (issue #1467).
            log.debug("이미 등록된 닉네임 — 검열 등록 생략: {}", nickname);
            return;
        }
        auditRepository.save(new NicknameAudit(nickname));
    }

    public void auditPending() {
        final long initialQueueSize = auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED);
        unauditedQueueDepth.set(initialQueueSize);
        log.info("닉네임 검열 시작: UNAUDITED 적체량 {}건", initialQueueSize);

        final Pageable pageable = PageRequest.of(0, properties.batchSize(), Sort.by("createdAt").ascending());
        List<NicknameAudit> batch = auditRepository.findByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED, pageable);
        int processedTotal = 0;

        while (!batch.isEmpty()) {
            int processed = batchProcessor.process(batch);
            processedTotal += processed;
            log.info("닉네임 검열 진행: 이번 배치 {}건, 누적 {}건", batch.size(), processedTotal);

            if (processed == 0 || batch.size() < properties.batchSize()) {
                break;
            }
            batch = auditRepository.findByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED, pageable);
        }

        log.info("닉네임 검열 완료: 총 {}건 처리", processedTotal);
    }
}
