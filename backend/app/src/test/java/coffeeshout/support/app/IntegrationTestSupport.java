package coffeeshout.support.app;

import coffeeshout.support.app.config.IntegrationTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestConfig.class)
public abstract class IntegrationTestSupport extends coffeeshout.support.IntegrationTestSupport {
}
