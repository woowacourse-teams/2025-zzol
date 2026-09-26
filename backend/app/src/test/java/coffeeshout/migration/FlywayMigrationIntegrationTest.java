package coffeeshout.migration;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameFeedback;
import coffeeshout.support.CommonTestSchedulerConfig;
import coffeeshout.support.app.config.IntegrationTestConfig;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Flyway가 실제로 구동되어 마이그레이션이 head까지 적용되는지 검증한다(#1606).
 * <p>
 * 다른 통합 테스트는 {@code flyway.enabled: false} + {@code ddl-auto: create}로 Flyway 경로를
 * 전혀 타지 않는다 — 그래서 Spring Boot 4 전환 때 Flyway 오토컨피그가 누락돼 마이그레이션이
 * 조용히 멈춘 것이 어떤 테스트에도 걸리지 않았다. 이 테스트는 운영과 동일하게
 * {@code flyway.enabled: true} + {@code ddl-auto: validate}로 <b>빈 DB</b>에 부팅한다.
 * <ul>
 *   <li>Flyway 오토컨피그가 없으면 → 마이그레이션이 안 돌아 빈 스키마에 validate가 깨지고 컨텍스트 로드 실패.
 *   <li>Flyway가 정상 구동되면 → 스키마를 head까지 만들고 validate 통과 → 컨텍스트 로드 성공.
 * </ul>
 * 공유 컨테이너(create 스키마)와 섞이면 flyway_schema_history 없는 DB를 baseline하는 오염이
 * 생기므로, 전용 fresh 컨테이너를 쓴다.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "spring.flyway.enabled=true",
            "spring.jpa.hibernate.ddl-auto=validate",
        })
@ActiveProfiles("test")
@Import({CommonTestSchedulerConfig.class, IntegrationTestConfig.class})
class FlywayMigrationIntegrationTest {

    private static final int VALKEY_PORT = 6379;

    @SuppressWarnings({"resource", "rawtypes"})
    static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("flyway_verify")
            .withUsername("test")
            .withPassword("test");

    @SuppressWarnings("resource")
    static final GenericContainer<?> valkey = new GenericContainer<>(DockerImageName.parse("valkey/valkey:alpine"))
            .withExposedPorts(VALKEY_PORT)
            .withCommand("valkey-server", "--save", "", "--appendonly", "no")
            .waitingFor(Wait.forListeningPort());

    static {
        mysql.start();
        valkey.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", valkey::getHost);
        registry.add("spring.data.redis.port", () -> valkey.getMappedPort(VALKEY_PORT));
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private NicknameFeedbackRepository feedbackRepository;

    @Test
    void Flyway가_구동되어_마이그레이션을_적용한다() {
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // 컨텍스트가 flyway.enabled=true + ddl-auto=validate로 빈 DB에 떴다는 것 자체가
        // Flyway가 스키마를 만들었다는 뜻이다(오토컨피그가 없으면 여기까지 오지 못한다).
        // 히스토리와 실제 스키마 양쪽을 직접 확인한다.
        final Integer flywayRuns =
                jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        final boolean v39Applied = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM flyway_schema_history WHERE version = '39' AND success = 1)",
                Boolean.class));
        final Integer dedupKeyColumn = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'zzolbot_monitor_run'
                   AND column_name = 'dedup_key'
                """, Integer.class);

        assertThat(flywayRuns).as("Flyway가 마이그레이션을 적용해야 한다").isGreaterThan(0);
        assertThat(v39Applied).as("V39가 성공적으로 적용돼야 한다").isTrue();
        assertThat(dedupKeyColumn).as("V39가 만든 dedup_key 컬럼이 존재해야 한다").isEqualTo(1);
    }

    /**
     * V48은 롤백을 위해 {@code player_name_feedback.ai_flagged}를 남겨 둔다. 엔티티는 이 칼럼을 모르므로
     * 기본값이 없으면 NOT NULL에 걸려 운영자의 허용·차단이 전부 실패한다.
     */
    @Test
    void 남겨둔_ai_flagged_칼럼이_있어도_피드백을_저장한다() {
        final NicknameFeedback saved = feedbackRepository.save(
                new NicknameFeedback("검증닉", AiConfidence.of(0.9), NicknameFeedback.OperatorDecision.BLOCKED, null));

        final Boolean aiFlagged = new JdbcTemplate(dataSource)
                .queryForObject(
                        "SELECT ai_flagged FROM player_name_feedback WHERE id = ?", Boolean.class, saved.getId());
        assertThat(aiFlagged).isTrue();
    }
}
