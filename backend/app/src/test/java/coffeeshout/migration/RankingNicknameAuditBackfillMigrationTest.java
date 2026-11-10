package coffeeshout.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * V49가 이번 달 랭킹에 오를 수 있는 이름만 검열 큐에 넣는지 실제 MySQL에서 본다(#1906).
 *
 * <p>운영 데이터에 INSERT하는 마이그레이션이라 SQL 문법 확인만으로는 부족하다. V48까지 올린 DB에 데이터를 심고 V49를
 * 적용한 뒤 무엇이 들어갔는지 확인한다. 다른 테스트와 스키마가 섞이지 않게 전용 컨테이너를 쓴다.
 */
class RankingNicknameAuditBackfillMigrationTest {

    @SuppressWarnings({"resource", "rawtypes"})
    private static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("backfill_verify")
            .withUsername("test")
            .withPassword("test");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void V48까지_올리고_데이터를_심은_뒤_V49를_적용한다() {
        mysql.start();
        // LAST_INSERT_ID()는 커넥션마다 따로라 시드가 커넥션 하나를 계속 쓴다.
        final SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword(), true);
        jdbc = new JdbcTemplate(dataSource);

        flyway(dataSource, "48").migrate();

        final long room = 방();
        플레이어(room, "새미니게임", 1);
        플레이어(room, "오래된이름", 40);
        플레이어(room, "이미검열", 1);
        검열_행("이미검열", "CLEAN");
        플레이어(room, "대소문자", 1);
        검열_행("대소문자".toUpperCase(), "CLEAN");

        final long member = 회원("회원새닉");
        룰렛_당첨(room, 플레이어(room, "방이름", 1, member), 1);
        final long oldMember = 회원("옛당첨");
        룰렛_당첨(room, 플레이어(room, "옛방이름", 40, oldMember), 40);

        flyway(dataSource, "49").migrate();
    }

    @Test
    void 최근_한_달의_플레이어_이름과_당첨_회원_닉네임만_UNAUDITED로_넣는다() {
        final List<String> unaudited =
                jdbc.queryForList("SELECT player_name FROM player_name_audit WHERE status = 'UNAUDITED'", String.class);

        // 오래된이름·옛당첨·옛방이름은 32일보다 오래돼 이번 달 랭킹에 오르지 않는다.
        // 이미검열·대소문자는 콜레이션이 같다고 보는 행이 이미 있다.
        assertThat(unaudited).containsExactlyInAnyOrder("새미니게임", "방이름", "회원새닉");
    }

    private static Flyway flyway(SingleConnectionDataSource dataSource, String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private static long 방() {
        jdbc.update("INSERT INTO room_session (join_code, room_status, created_at) VALUES ('BKFL', 'PLAYING', NOW())");
        return lastId();
    }

    private static long 플레이어(long room, String name, int daysAgo) {
        return 플레이어(room, name, daysAgo, null);
    }

    private static long 플레이어(long room, String name, int daysAgo, Long userId) {
        jdbc.update(
                "INSERT INTO player (room_session_id, player_name, player_type, created_at, user_id)"
                        + " VALUES (?, ?, 'GUEST', NOW() - INTERVAL ? DAY, ?)",
                room,
                name,
                daysAgo,
                userId);
        return lastId();
    }

    private static long 회원(String nickname) {
        jdbc.update(
                "INSERT INTO app_user (user_code, nickname, created_at, updated_at)"
                        + " VALUES (LEFT(UUID(), 5), ?, NOW(), NOW())",
                nickname);
        return lastId();
    }

    private static void 룰렛_당첨(long room, long winner, int daysAgo) {
        jdbc.update(
                "INSERT INTO roulette_result (room_session_id, winner_id, winner_probability, created_at)"
                        + " VALUES (?, ?, 10, NOW() - INTERVAL ? DAY)",
                room,
                winner,
                daysAgo);
    }

    private static void 검열_행(String name, String status) {
        jdbc.update(
                "INSERT INTO player_name_audit (player_name, status, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
                name,
                status);
    }

    private static long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
