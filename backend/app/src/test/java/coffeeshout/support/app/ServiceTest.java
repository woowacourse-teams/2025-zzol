package coffeeshout.support.app;

import coffeeshout.support.app.config.ServiceTestConfig;
import org.springframework.context.annotation.Import;

@Import(ServiceTestConfig.class)
public abstract class ServiceTest extends coffeeshout.support.ServiceTest {
}
