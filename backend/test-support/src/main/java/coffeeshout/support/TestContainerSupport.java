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
    private static final String MODULE_DB = System.getProperty("test.db.name", BASE_DB);
    private static final int MODULE_REDIS_DB = Integer.parseInt(System.getProperty("test.redis.db", "0"));

    // 컨테이너 reuse를 끈다(JVM별 독립 컨테이너). ADR-0013은 모듈별 DB·Redis 인덱스 격리로 reuse
    // 병렬 안전을 전제했으나, 병렬 모듈 테스트(parallel=true·workers=4)가 단일 공유 컨테이너를 동시에
    // 두드리면 그 전제가 깨진다: 공유 Valkey는 스트림 처리가 지연되며 비동기로 늦게 발행된 stale
    // 이벤트(이미 정리된 방 참조)가 컨슈머 풀을 잠식하고, 공유 MySQL은 경합으로 처리가 밀려, 게임
    // 통합테스트가 간헐 타임아웃한다(로컬 재현으로 확인 — reuse off 시 통과, valkey만 off로는 MySQL
    // 경합 잔존으로 실패). 스트림/컨텍스트 격리의 근본 개선은 별도 작업(#1361/#1369)이며, 그 전까지
    // reuse를 비활성화한다. ff452199의 기동 시간 최적화를 일부 되돌리는 트레이드오프.
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
        if (!MODULE_DB.equals(BASE_DB)) {
            createDatabaseIfAbsent(MODULE_DB);
        }
    }

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> mysql.getJdbcUrl().replace("/" + BASE_DB, "/" + MODULE_DB));
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", valkey::getHost);
        registry.add("spring.data.redis.port", () -> valkey.getMappedPort(VALKEY_PORT));
        registry.add("spring.data.redis.database", () -> MODULE_REDIS_DB);
    }

    private static void createDatabaseIfAbsent(String dbName) {
        try {
            var result = mysql.execInContainer(
                    "mysql",
                    "--user=root",
                    "--password=" + mysql.getPassword(),
                    "--execute=CREATE DATABASE IF NOT EXISTS `" + dbName + "`"
                            + "; GRANT ALL PRIVILEGES ON `" + dbName + "`.* TO '" + mysql.getUsername() + "'@'%'"
                            + "; FLUSH PRIVILEGES"
            );
            if (result.getExitCode() != 0) {
                throw new RuntimeException(result.getStderr());
            }
            log.info("모듈 테스트 DB 생성: {}", dbName);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("모듈 테스트 DB 생성 실패: " + dbName, e);
        }
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
