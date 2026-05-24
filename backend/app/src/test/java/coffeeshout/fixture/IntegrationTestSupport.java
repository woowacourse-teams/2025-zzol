package coffeeshout.fixture;

import coffeeshout.global.config.IntegrationTestConfig;
import org.springframework.context.annotation.Import;

@Import(IntegrationTestConfig.class)
public abstract class IntegrationTestSupport extends coffeeshout.support.IntegrationTestSupport {
}
