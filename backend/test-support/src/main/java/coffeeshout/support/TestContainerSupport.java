package coffeeshout.support;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands.FlushOption;
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
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci",
                    "--max_connections=500");

    protected static final GenericContainer<?> valkey = new GenericContainer<>(
            DockerImageName.parse("valkey/valkey:alpine"))
            .withExposedPorts(VALKEY_PORT)
            .withCommand("valkey-server", "--appendonly", "yes")
            .withEnv("VALKEY_DISABLE_COMMANDS", "CONFIG,SHUTDOWN,DEBUG")
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
    void cleanRedis() {
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.flushAll(FlushOption.SYNC);
            log.debug("Redis flushed");
        } catch (Exception e) {
            log.warn("Failed to flush Redis: {}", e.getMessage());
        }
    }

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
