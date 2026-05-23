package coffeeshout.room;

import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.application.service.DelayedRoomRemovalService;
import coffeeshout.support.ServiceTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

public abstract class StreamMockedServiceTest extends ServiceTest {

    @MockitoBean
    protected StreamPublisher streamPublisher;

    @MockitoSpyBean
    protected DelayedRoomRemovalService delayedRoomRemovalService;
}
