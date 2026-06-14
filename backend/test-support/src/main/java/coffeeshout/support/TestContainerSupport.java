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

    // 컨테이너 reuse를 켜고(JVM 간 단일 공유 MySQL·Valkey), 모듈별 DB·Redis 인덱스 격리(ADR-0013)로
    // 병렬 모듈 테스트(parallel=true·workers=4)의 데이터 충돌을 막는다. reuse-off(이슈 #1402)는 게임
    // 통합테스트 플레이키의 근본 원인을 공유 컨테이너 자원 경합으로 지목했으나, 이후 검증에서 reuse 자체는
    // 원인이 아님이 확인됐다 — 실제 결함은 #1411(게임 통합테스트 subscribe 등록 완료 대기, 타이밍)이
    // 해소했다. 따라서 reuse 비활성화 결정을 철회하고 컨테이너 재사용 + 모듈별 격리를 복구한다(이슈 #1417).
    protected static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName(BASE_DB)
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci",
                    "--max_connections=500", "--innodb_flush_log_at_trx_commit=2", "--sync_binlog=0")
            .withReuse(true);

    protected static final GenericContainer<?> valkey = new GenericContainer<>(
            DockerImageName.parse("valkey/valkey:alpine"))
            .withExposedPorts(VALKEY_PORT)
            .withCommand("valkey-server", "--save", "", "--appendonly", "no", "--loglevel", "warning")
            .withEnv("VALKEY_DISABLE_COMMANDS", "CONFIG,SHUTDOWN,DEBUG")
            .withReuse(true)
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
