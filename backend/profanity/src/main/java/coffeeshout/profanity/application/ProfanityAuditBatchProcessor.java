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
            // 배치 닉네임 중 이미 검열 완료(terminal) 행을 가진 것들을 한 번에 조회한다 (건별 조회 N+1 회피).
            final Set<String> nicknamesWithTerminal = auditRepository.findNicknamesWithTerminalStatus(nicknames);

            final List<NicknameAudit> toPromote = new ArrayList<>();
            final List<NicknameAudit> redundant = new ArrayList<>();
            for (final NicknameAudit entity : batch) {
                final NicknameAuditResult result = resultMap.get(entity.getNickname());
                // 같은 닉네임의 검열 완료(terminal) 행이 이미 있으면 이 UNAUDITED는 #1467 fix 이전에 생긴
                // 잔존 중복 재등록이다(register의 "이름당 1행" 불변식 위반). 승격하면 대상 상태가 같을 때
                // uq_player_name_audit_name_status에 충돌해 배치 전체가 롤백되고 매 tick 재크래시하며,
                // 다를 때는 (예: 기존 CLEAN + 신규 FLAGGED) 모순된 감사 이력과 autoBlock 오발동을 남긴다.
                // 어느 쪽이든 승격 대신 제거한다 — 기존 terminal 행이 권위 있는 판정을 이미 보유한다.
                if (result != null && nicknamesWithTerminal.contains(entity.getNickname())) {
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
