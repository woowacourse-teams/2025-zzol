package coffeeshout.support.app;

import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import coffeeshout.room.application.service.DelayedRoomRemovalService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

public abstract class StreamMockedServiceTest extends ServiceTest {

    @MockitoBean
    protected StreamPublisher streamPublisher;

    @MockitoSpyBean
    protected DelayedRoomRemovalService delayedRoomRemovalService;

    /**
     * 컨텍스트 캐시 공유를 위해 베이스에서 spy로 고정한다. 개별 테스트 클래스에 선언하면 클래스마다 새 컨텍스트가 생성된다.
     */
    @MockitoSpyBean
    protected StreamTracePropagator streamTracePropagator;
}
