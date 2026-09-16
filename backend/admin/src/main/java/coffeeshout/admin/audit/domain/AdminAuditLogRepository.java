package coffeeshout.admin.audit.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminAuditLogRepository {

    AdminAuditLog save(AdminAuditLog auditLog);

    /** 최근 조치부터. 감사 로그는 언제나 "방금 무슨 일이 있었나"로 읽는다. */
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 실행자와 결과와 기간으로 걸러 본다. 셋 다 없으면 전체다.
     *
     * <p>페이지네이션만 있던 시절에는 "누가 그 IP 를 풀었나"를 알려면 전 페이지를 눈으로
     * 훑어야 했다. 조치가 쌓일수록 감사 로그의 쓸모가 줄어드는 구조였다.
     *
     * @param actorEmail 부분 일치. 운영자는 이메일 전체가 아니라 앞자리만 기억한다
     * @param result     실패한 조치만 보는 일이 실제로 잦다. 되돌려야 할 것이 거기 있다
     * @param from       이 시각부터. 끝을 받지 않는 것은 감사 로그를 늘 "지금까지"로 읽기 때문이다
     */
    Page<AdminAuditLog> search(String actorEmail, AdminAuditResult result, Instant from, Pageable pageable);

    /**
     * 기간 안의 조치를 한 줄씩. 종류, 담당자, 일자별 집계가 이 한 번의 조회에서 나온다.
     *
     * <p>세 그래프가 <b>같은 순간의 같은 조치들</b>을 말해야 해서 나눠 돌리지 않는다.
     * 조치가 초당 수십 건 쌓이는 테이블이 아니라 한 번에 읽어도 부담이 없고, 기간에는
     * 상한을 둔다.
     */
    List<AdminAuditLog> findAllByCreatedAtBetween(Instant from, Instant to);
}
