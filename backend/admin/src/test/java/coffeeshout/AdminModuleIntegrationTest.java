package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.support.IntegrationTestSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = AdminModuleTestApplication.class, webEnvironment = WebEnvironment.MOCK)
@Import(ServiceTestConfig.class)
public abstract class AdminModuleIntegrationTest extends IntegrationTestSupport {
}
