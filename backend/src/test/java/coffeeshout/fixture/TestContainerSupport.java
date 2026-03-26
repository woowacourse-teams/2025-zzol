package coffeeshout.fixture;

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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


public abstract class TestContainerSupport {

    private static final Logger log = LoggerFactory.getLogger(TestContainerSupport.class);
    private static final int VALKEY_PORT = 6379;

    protected static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("coffee_shout_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
            .withReuse(true);

    protected static final GenericContainer<?> valkey = new GenericContainer<>(
            DockerImageName.parse("valkey/valkey:alpine"))
            .withExposedPorts(VALKEY_PORT)
            .withCommand("valkey-server", "--appendonly", "yes")
            .withEnv("VALKEY_DISABLE_COMMANDS", "CONFIG,SHUTDOWN,DEBUG")
            .withReuse(true)
            .waitingFor(Wait.forListeningPort())
            .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("VALKEY"));

    static {
        mysql.start();
        valkey.start();
    }

    @Autowired
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
    void cleanUp() {
        cleanDatabase();
        cleanRedis();
    }

    void cleanRedis() {
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
            log.debug("Redis flushed");
        } catch (Exception e) {
            log.warn("Failed to flush Redis: {}", e.getMessage());
        }
    }

    /**
     * 모든 테이블을 TRUNCATE한다.
     * <p>
     * @Transactional 없는 테스트(E2E 등)에서 @BeforeEach에 호출한다.
     * @Transactional 테스트는 DB 롤백이 자동으로 처리되므로 호출할 필요 없다.
     */
    protected void cleanDatabase() {
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
