package coffeeshout.profanity.infra.persistence.audit;

import coffeeshout.profanity.domain.audit.NicknameAudit;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class NicknameAuditBulkUpdaterImpl implements NicknameAuditBulkUpdater {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final JdbcTemplate jdbcTemplate;

    /**
     * 준영속 엔티티를 {@code saveAll}로 넘기면 JPA merge가 갱신 전 영속 인스턴스를 얻으려고 건별 SELECT를
     * 먼저 날린다. JDBC 배치 UPDATE로 곧장 쓰면 이 SELECT가 통째로 없어진다.
     *
     * <p>rewriteBatchedStatements=true(운영 설정, {@code database.yml})면 드라이버가 배치를 multi-row
     * UPDATE로 재작성하고 executeBatch가 실제 행수 대신 SUCCESS_NO_INFO(-2)를 반환한다
     * ({@code ProfanityWordRepositoryImpl.bulkInsertIgnore}와 같은 함정). 그래서 반환값으로 갱신 행 수를
     * 세지 않고 void로 둔다.
     *
     * <p>매칭된 행 수를 안 보므로 그 사이 지워진 행은 조용히 넘어간다. {@code saveAll}은 merge가 그 행을
     * 다시 INSERT해 되살렸는데, 지운 행을 되살리지 않는 쪽이 맞다고 보고 그대로 둔다. 대신 호출자의 처리
     * 건수와 판정 메트릭이 그만큼 실제보다 크게 세어진다.
     *
     * <p>{@link NicknameAudit#complete}를 거쳐 status·confidence·reason·audited_at이 모두 채워진
     * 엔티티만 넘긴다. 갱신도 이 네 컬럼만 하고 attempt_count·created_at·player_name은 건드리지 않는다.
     * attempt_count는 일부러 뺐다: {@code incrementAttemptCount}가 JPQL 벌크 UPDATE로 DB에서 직접
     * 올리므로, 이 메서드에 넘어오는 준영속 엔티티가 든 값은 낡았을 수 있다. 예전 {@code saveAll}은 그
     * 낡은 값으로 덮어썼다.
     */
    @Override
    public void bulkUpdateAuditResults(List<NicknameAudit> entities) {
        if (entities.isEmpty()) {
            return;
        }
        final String sql =
                "UPDATE player_name_audit SET status = ?, confidence = ?, reason = ?, audited_at = ? WHERE id = ?";
        // Calendar 없이 setTimestamp를 쓰면 JVM 기본 타임존(KST 등)으로 해석돼, Hibernate가 Instant 컬럼을
        // 읽고 쓸 때 쓰는 UTC 기준과 어긋나 시간이 몇 시간씩 밀린다. Calendar는 상태를 갖는 mutable 객체라
        // 배치 항목마다 새로 만든다.
        jdbcTemplate.batchUpdate(sql, entities, 500, (ps, entity) -> {
            ps.setString(1, entity.getStatus().name());
            ps.setBigDecimal(2, entity.getConfidence().value());
            ps.setString(3, entity.getReason());
            ps.setTimestamp(4, Timestamp.from(entity.getAuditedAt()), Calendar.getInstance(UTC));
            ps.setLong(5, entity.getId());
        });
    }
}
