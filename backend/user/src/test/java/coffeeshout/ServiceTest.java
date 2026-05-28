package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.global.nickname.ProfanityChecker;
import coffeeshout.support.TestContainerSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = UserModuleTestApplication.class)
@Import(ServiceTestConfig.class)
@ActiveProfiles("test")
@Transactional
public abstract class ServiceTest extends TestContainerSupport {

    @MockitoBean
    protected ApplicationEventPublisher eventPublisher;

    @MockitoBean
    protected ProfanityChecker profanityChecker;
}
