package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.support.ServiceTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = RoomModuleTestApplication.class)
@Import(ServiceTestConfig.class)
public abstract class RoomModuleServiceTest extends ServiceTest {
}
