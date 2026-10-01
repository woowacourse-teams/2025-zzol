package coffeeshout.admin.audit.infra.persistence;

import coffeeshout.admin.audit.domain.AdminAuditLog;
import coffeeshout.admin.audit.domain.AdminAuditLogRepository;
import coffeeshout.admin.audit.domain.AdminAuditResult;
import coffeeshout.global.persistence.LikePattern;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogJpaRepository extends Repository<AdminAuditLog, Long>, AdminAuditLogRepository {

    /**
     * 파라미터가 null 이면 그 조건을 건너뛴다.
     *
     * <p>QueryDSL 로 짜지 않았다. 조건이 셋뿐이고 전부 단순 비교라, 동적 쿼리 빌더를
     * 들이면 읽어야 할 코드가 오히려 늘어난다. 조건이 더 붙거나 조인이 생기면 그때 옮긴다.
     *
     * <p>정렬을 쿼리에 박는다. {@code Pageable} 의 정렬에 맡기면 호출하는 쪽이 정렬을
     * 안 주었을 때 순서가 DB 마음대로가 되고, 감사 로그는 순서가 곧 뜻이다.
     *
     * <p>패턴을 쿼리 안에서 {@code CONCAT} 으로 만들지 않는다. 그러면 검색어에 들어 있는
     * {@code %} 와 {@code _} 가 와일드카드로 읽힌다. 관리자 이메일에는 밑줄이 흔해서
     * {@code mj_admin} 을 찾으면 {@code mjXadmin} 까지 걸렸다.
     */

    /**
     * 검색어를 패턴으로 감싸 넘긴다.
     *
     * <p>감싸는 일을 컨트롤러에 두지 않는다. 그러면 이 저장소의 계약이 "검색어"가 아니라
     * "LIKE 패턴"이 되어, 부르는 쪽마다 이스케이프를 기억해야 한다. 한 곳이라도 잊으면
     * 그 화면만 조용히 와일드카드가 먹힌다.
     */
    @Override
    default Page<AdminAuditLog> search(String actorEmail, AdminAuditResult result, Instant from, Pageable pageable) {
        return searchByPattern(actorEmail == null ? null : LikePattern.contains(actorEmail), result, from, pageable);
    }

    @Query("""
            SELECT log FROM AdminAuditLog log
            WHERE (:pattern IS NULL OR log.actorEmail LIKE :pattern ESCAPE '!')
              AND (:result IS NULL OR log.result = :result)
              AND (:from IS NULL OR log.createdAt >= :from)
            ORDER BY log.createdAt DESC
            """)
    Page<AdminAuditLog> searchByPattern(
            @Param("pattern") String pattern,
            @Param("result") AdminAuditResult result,
            @Param("from") Instant from,
            Pageable pageable);
}
