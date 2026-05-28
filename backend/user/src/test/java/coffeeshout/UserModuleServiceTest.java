package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = UserModuleTestApplication.class)
@Import(ServiceTestConfig.class)
public abstract class UserModuleServiceTest extends coffeeshout.support.ServiceTest {
}
