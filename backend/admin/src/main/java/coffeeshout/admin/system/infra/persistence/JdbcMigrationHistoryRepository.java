package coffeeshout.admin.system.infra.persistence;

import coffeeshout.admin.system.domain.MigrationHistoryRepository;
import coffeeshout.admin.system.domain.MigrationRecord;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@code flyway_schema_history} 를 직접 읽는다.
 *
 * <p>Flyway 빈을 주입받지 않는다. Flyway 는 {@code :app} 모듈에만 있고 {@code :admin} 은
 * 그쪽을 보지 않는다. 이 조회 하나 때문에 모듈 간선을 새로 만들 이유가 없다.
 *
 * <p>JPA 엔티티로도 만들지 않았다. 이 테이블은 <b>우리 스키마가 아니라 Flyway 의 것</b>이다.
 * 엔티티를 두면 ddl-auto 가 도는 환경에서 하이버네이트가 이 테이블을 만들려 들고, 그러면
 * Flyway 가 나중에 자기 테이블을 보고 혼란스러워한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcMigrationHistoryRepository implements MigrationHistoryRepository {

    private static final String TABLE = "flyway_schema_history";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean exists() {
        final Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, TABLE);
        return count != null && count > 0;
    }

    @Override
    public List<MigrationRecord> findAll(int limit) {
        if (!exists()) {
            return List.of();
        }
        try {
            return jdbcTemplate.query(
                    """
                    SELECT version, description, type, installed_on, success, execution_time
                      FROM flyway_schema_history
                     ORDER BY installed_rank DESC
                     LIMIT ?
                    """,
                    (rs, rowNum) -> new MigrationRecord(
                            rs.getString("version"),
                            rs.getString("description"),
                            rs.getString("type"),
                            toInstant(rs.getTimestamp("installed_on")),
                            rs.getBoolean("success"),
                            (Integer) rs.getObject("execution_time")),
                    limit);
        } catch (DataAccessException e) {
            // exists() 와 조회 사이에 테이블이 사라지는 경우는 사실상 없지만, 여기서 터지면
            // 시스템 화면 전체가 오류로 덮인다. 마이그레이션 이력 하나 때문에 DLQ 목록까지
            // 못 보게 만들지 않는다.
            log.warn("[System] 마이그레이션 이력 조회 실패", e);
            return List.of();
        }
    }

    @Override
    public Optional<String> currentVersion() {
        return findAll(1).stream().map(MigrationRecord::version).findFirst();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
