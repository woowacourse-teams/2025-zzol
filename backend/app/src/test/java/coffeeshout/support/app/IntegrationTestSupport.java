package coffeeshout.support.app;

import coffeeshout.support.app.config.IntegrationTestConfig;
import org.springframework.context.annotation.Import;

@Import(IntegrationTestConfig.class)
public abstract class IntegrationTestSupport extends coffeeshout.support.IntegrationTestSupport {
}
