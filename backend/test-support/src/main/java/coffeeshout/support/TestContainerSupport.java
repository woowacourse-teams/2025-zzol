package coffeeshout.support;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public abstract class TestContainerSupport {

    private static final Logger log = LoggerFactory.getLogger(TestContainerSupport.class);
    private static final int VALKEY_PORT = 6379;
    private static final String BASE_DB = "zzol_test";

    // 컨테이너 reuse를 끄고 JVM별 독립 컨테이너(=물리적 자원 격리)를 쓴다 — 영구 결정(이슈 #1402).
    // 단일 공유 컨테이너 + 모듈별 DB·Redis 인덱스 격리(ADR-0013)는 데이터 충돌만 막을 뿐, 병렬 모듈
    // 테스트(parallel=true·workers=4)가 한 MySQL·Valkey 프로세스를 동시에 두드릴 때 생기는 자원 경합
    // (처리량 한계·지연 변동)은 막지 못한다 — 타이밍 민감한 게임 통합테스트가 간헐 awaitility
    // 타임아웃했다. 스트림/컨텍스트 개선(#1361/#1369)이 이미 머지된 상태에서도 reuse-on은 플레이키로
    // 재현돼, 물리적 격리만이 유효 해법임을 확정했다. 그에 따라 무효가 된 모듈별 DB/Redis 격리는 제거했다.
    protected static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName(BASE_DB)
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci",
                    "--max_connections=500", "--innodb_flush_log_at_trx_commit=2", "--sync_binlog=0");

    protected static final GenericContainer<?> valkey = new GenericContainer<>(
            DockerImageName.parse("valkey/valkey:alpine"))
            .withExposedPorts(VALKEY_PORT)
            .withCommand("valkey-server", "--save", "", "--appendonly", "no", "--loglevel", "warning")
            .withEnv("VALKEY_DISABLE_COMMANDS", "CONFIG,SHUTDOWN,DEBUG")
            .waitingFor(Wait.forListeningPort())
            .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("VALKEY"));

    static {
        mysql.start();
        valkey.start();
    }

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", valkey::getHost);
        registry.add("spring.data.redis.port", () -> valkey.getMappedPort(VALKEY_PORT));
    }

    @BeforeEach
    void cleanRedis() {
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
            log.debug("Redis flushed");
        } catch (Exception e) {
            log.warn("Failed to flush Redis: {}", e.getMessage());
        }
    }

    protected void cleanDatabase() {
        if (jdbcTemplate == null) {
            return;
        }
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
            for (String table : tables) {
                jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
            }
            log.debug("Database truncated ({} tables)", tables.size());
        } catch (Exception e) {
            log.warn("Failed to clean database", e);
        } finally {
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            } catch (Exception restoreEx) {
                log.error("Failed to restore FOREIGN_KEY_CHECKS to 1", restoreEx);
            }
        }
    }
}
