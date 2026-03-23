package coffeeshout.global;

import coffeeshout.global.redis.stream.StreamPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class StreamMockedServiceTest extends ServiceTest {

    @MockitoBean
    protected StreamPublisher streamPublisher;

}
