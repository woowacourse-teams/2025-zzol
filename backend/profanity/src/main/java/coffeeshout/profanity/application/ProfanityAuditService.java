package coffeeshout.profanity.application;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.global.nickname.NicknameSubmittedEvent;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 닉네임 저장 트랜잭션에 참여해 검열 대기 행을 함께 커밋한다 (트랜잭셔널 아웃박스, #1618).
     * <p>
     * AFTER_COMMIT + REQUIRES_NEW였을 때는 닉네임이 커밋된 뒤 별개 트랜잭션으로 큐에 적재해서,
     * 그 사이 인스턴스가 죽으면 해당 닉네임이 검열 큐에 영영 들어가지 않았다. 미검열 닉네임을
     * 다시 주워담는 복구 스윕도 없어 유실이 곧 영구 누락이었다.
     */
    @EventListener
    @Transactional
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
        // save가 아니라 충돌 무시 INSERT다. 위 조회를 통과한 동시 등록 둘이 겹치면 유니크 제약에
        // 걸리는데, 이제 호출자(닉네임 변경·룰렛 결과 저장) 트랜잭션 안이라 예외가 나면 그쪽까지
        // 롤백된다. 충돌은 "이미 큐에 있다"는 뜻이라 무시해도 손실이 없다.
        auditRepository.insertUnaudited(nickname, Instant.now());
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
