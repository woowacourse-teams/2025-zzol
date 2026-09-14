package coffeeshout.admin.audit.application;

import coffeeshout.admin.audit.domain.AdminAuditLog;
import coffeeshout.admin.audit.domain.AdminAuditLogRepository;
import coffeeshout.admin.audit.domain.AdminAuditResult;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final Clock clock;

    /**
     * 감사 기록을 남긴다.
     *
     * <p>{@code REQUIRES_NEW}로 별도 트랜잭션에서 커밋한다. 조치가 실패해 업무 트랜잭션이
     * 롤백되면 같은 트랜잭션에 있던 감사 기록도 함께 사라진다. 실패한 시도야말로 남아야 하므로
     * 트랜잭션을 분리한다.
     *
     * <p>기록 자체가 실패해도 예외를 밖으로 던지지 않는다. 감사 로그가 원래 요청을 죽이면
     * 백오피스 전체가 감사 테이블 가용성에 묶인다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String actorEmail,
            String action,
            String targetType,
            String targetId,
            String detail,
            AdminAuditResult result) {
        try {
            adminAuditLogRepository.save(
                    AdminAuditLog.of(actorEmail, action, targetType, targetId, detail, result, clock.instant()));
        } catch (Exception e) {
            log.error("관리자 감사 로그 기록 실패: actor={} action={} result={}", forLog(actorEmail), forLog(action), result, e);
        }
    }

    /**
     * 로그에 넣기 전에 줄바꿈을 지운다.
     *
     * <p>값에 개행이 들어 있으면 로그 한 줄이 여러 줄로 쪼개지고, 그 틈에 진짜처럼 생긴
     * 가짜 로그 줄을 끼워 넣을 수 있다. 하필 이 자리는 <b>감사 로그가 실패했을 때</b> 남기는
     * 마지막 흔적이라, 여기가 오염되면 무슨 일이 있었는지 되짚을 방법이 사라진다.
     *
     * <p>{@code actorEmail} 은 검증된 구글 토큰에서 오고 {@code action} 은 서버가 정한 매핑
     * 패턴이라 지금은 개행이 들어올 경로가 없다. 다만 이 메서드는 조치가 <b>실패한</b> 경로에서
     * 불리고, 그때 어떤 값이 넘어오는지는 호출부가 늘어날수록 보장하기 어려워진다.
     */
    private static String forLog(String value) {
        // null 분기를 두지 않는다. 분기가 있으면 "어떤 경로에서는 원본이 그대로 나간다"가
        // 참이 되고, 그 사실은 사람이 읽을 때도 정적 분석이 읽을 때도 똑같이 걸린다.
        // null 이면 "null" 이 찍히는데 로그에서는 그걸로 충분하다.
        return String.valueOf(value).replaceAll("[\\r\\n]", "_");
    }
}
