package coffeeshout;

import coffeeshout.config.IntegrationTestConfig;
import coffeeshout.support.IntegrationTestSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = GameModuleTestApplication.class, webEnvironment = WebEnvironment.MOCK)
@Import(IntegrationTestConfig.class)
public abstract class GameModuleIntegrationTest extends IntegrationTestSupport {
}
