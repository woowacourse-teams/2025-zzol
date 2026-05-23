package coffeeshout.support;

import coffeeshout.support.config.BaseServiceTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(BaseServiceTestConfig.class)
@ActiveProfiles("test")
@Transactional
public abstract class ServiceTest extends TestContainerSupport {

    @MockitoBean
    protected ApplicationEventPublisher eventPublisher;
}
