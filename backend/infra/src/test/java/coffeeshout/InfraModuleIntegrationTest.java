package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.global.outbox.OutboxAfterCommitRelay;
import coffeeshout.global.redis.config.RedisStreamResilienceTestConfig;
import coffeeshout.support.IntegrationTestSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * RedisStreamResilienceTestConfig 임포트로 RecordingDummyEventConsumer가 minigame 스트림의
 * 라이브 리스너로 모든 infra 통합 테스트 컨텍스트에 상주한다. 개별 테스트 클래스에 @Import를 선언하면
 * 클래스마다 새 컨텍스트가 생성되므로 캐시 공유를 위해 베이스에서 임포트한다.
 */
@SpringBootTest(classes = InfraModuleTestApplication.class, webEnvironment = WebEnvironment.MOCK)
@Import({ServiceTestConfig.class, RedisStreamResilienceTestConfig.class})
public abstract class InfraModuleIntegrationTest extends IntegrationTestSupport {

    /**
     * 컨텍스트 캐시 공유를 위해 베이스에서 spy로 고정한다. 개별 테스트 클래스에 @MockitoSpyBean을 선언하면
     * 클래스마다 새 컨텍스트가 생성된다. spy는 기본적으로 실제 동작을 위임하므로 다른 테스트에 영향이 없다.
     */
    @MockitoSpyBean
    protected OutboxAfterCommitRelay outboxAfterCommitRelay;
}
