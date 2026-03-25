package coffeeshout.global;

import coffeeshout.fixture.TestContainerSupport;
import coffeeshout.global.config.ServiceTestConfig;
import coffeeshout.room.application.service.DelayedRoomRemovalService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(ServiceTestConfig.class)
@ActiveProfiles("test")
@Transactional
public abstract class ServiceTest extends TestContainerSupport {

    @MockitoBean
    protected ApplicationEventPublisher eventPublisher;

    @MockitoSpyBean
    protected DelayedRoomRemovalService delayedRoomRemovalService;

}
