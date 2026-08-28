package coffeeshout.support.app;

import coffeeshout.config.IntegrationSchedulerTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationSchedulerTestConfig.class)
public abstract class IntegrationTestSupport extends coffeeshout.support.IntegrationTestSupport {}
