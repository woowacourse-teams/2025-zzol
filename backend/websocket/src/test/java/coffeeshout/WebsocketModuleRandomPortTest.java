package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.support.IntegrationTestSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = WebsocketModuleTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ServiceTestConfig.class)
public abstract class WebsocketModuleRandomPortTest extends IntegrationTestSupport {
}
