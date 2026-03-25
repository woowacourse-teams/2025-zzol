package coffeeshout.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("test")
public class TestContainerConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestContainerConfig.class);
    private static final Object CONTAINER_LOCK = new Object();
    private static volatile GenericContainer<?> valkeyContainer;

    private static final int VALKEY_PORT = 6379;

    static {
        initializeContainer();
    }

    /**
     * 멀티쓰레드 환경에서 컨테이너가 한 번만 초기화되도록 동기화 처리
     */

    private static void initializeContainer() {
        synchronized (CONTAINER_LOCK) {
            if (valkeyContainer == null) {
                try {
                    logger.info("Initializing Valkey TestContainer...");
                    
                    valkeyContainer = new GenericContainer<>(DockerImageName.parse("valkey/valkey:latest"))
                            .withExposedPorts(VALKEY_PORT)
                            .withCommand("valkey-server", "--appendonly", "yes")
                            .withEnv("VALKEY_DISABLE_COMMANDS", "CONFIG,SHUTDOWN,DEBUG,EVAL,SCRIPT") // FLUSHALL 테스트용 허용
                            .withReuse(true)
                            .waitingFor(Wait.forListeningPort())
                            .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("VALKEY"));

                    startContainerSafely();

                    logger.info("Valkey TestContainer initialized successfully on {}:{}", 
                              valkeyContainer.getHost(), valkeyContainer.getMappedPort(VALKEY_PORT));
                } catch (Exception e) {
                    logger.error("Failed to initialize Valkey TestContainer", e);
                    throw new RuntimeException("TestContainer initialization failed", e);
                }
            }
        }
    }

    private static void startContainerSafely() {
        try {
            if (!valkeyContainer.isRunning()) {
                valkeyContainer.start();
                logger.info("Valkey container started successfully");
            } else {
                logger.info("Reusing existing Valkey container");
            }
        } catch (Exception e) {
            logger.error("Failed to start Valkey container", e);
            throw new RuntimeException("Container startup failed", e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (valkeyContainer != null && valkeyContainer.isRunning()) {
            String host = valkeyContainer.getHost();
            Integer port = valkeyContainer.getMappedPort(VALKEY_PORT);
            
            logger.debug("Configuring Redis properties: host={}, port={}", host, port);
            
            registry.add("spring.data.redis.host", () -> host);
            registry.add("spring.data.redis.port", () -> port);
        } else {
            logger.error("Valkey container is not running, cannot configure properties");
        }
    }

    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        if (valkeyContainer == null || !valkeyContainer.isRunning()) {
            throw new IllegalStateException("Valkey container is not available for connection factory");
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                valkeyContainer.getHost(),
                valkeyContainer.getMappedPort(VALKEY_PORT)
        );
        factory.setValidateConnection(true);
        factory.afterPropertiesSet();

        logger.info("Created Redis connection factory for {}:{}", 
                   valkeyContainer.getHost(), valkeyContainer.getMappedPort(VALKEY_PORT));
        
        return factory;
    }

    @PostConstruct
    public void flushAllData() {
        if (valkeyContainer != null && valkeyContainer.isRunning()) {
            try {
                logger.debug("Flushing all Valkey data before test execution...");
                valkeyContainer.execInContainer("valkey-cli", "FLUSHALL");
                logger.debug("Valkey data flush completed");
            } catch (Exception e) {
                logger.warn("Failed to flush Valkey data at startup: {}", e.getMessage());
            }
        } else {
            logger.warn("Valkey container not available for data flush");
        }
    }

    @PreDestroy
    public void cleanup() {
        if (valkeyContainer != null && valkeyContainer.isRunning()) {
            try {
                logger.info("Performing cleanup on TestContainer destruction...");
                valkeyContainer.execInContainer("valkey-cli", "FLUSHALL");
            } catch (Exception e) {
                logger.warn("Failed to cleanup Valkey data on destroy: {}", e.getMessage());
            }
        }
    }

    public static boolean isContainerRunning() {
        return valkeyContainer != null && valkeyContainer.isRunning();
    }

    public static String getContainerHost() {
        return valkeyContainer != null ? valkeyContainer.getHost() : null;
    }

    public static Integer getContainerPort() {
        return valkeyContainer != null ? valkeyContainer.getMappedPort(VALKEY_PORT) : null;
    }
}
