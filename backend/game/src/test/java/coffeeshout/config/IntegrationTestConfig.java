package coffeeshout.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * game 모듈 IT 설정 — 스케줄러 미러는 {@link IntegrationSchedulerTestConfig}(app IT와 공유),
 * 외부 포트 목은 game 전용인 {@link ExternalPortMockConfig}가 담당한다.
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
@Import({ExternalPortMockConfig.class, IntegrationSchedulerTestConfig.class})
public class IntegrationTestConfig {}
