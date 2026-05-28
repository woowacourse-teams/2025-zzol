package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import org.springframework.context.annotation.Import;

@Import(ServiceTestConfig.class)
public abstract class ServiceTest extends coffeeshout.support.ServiceTest {
}
