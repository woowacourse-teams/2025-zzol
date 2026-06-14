package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.report.ratelimit.ReportRateLimitStore;
import coffeeshout.support.IntegrationTestSupport;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = AdminModuleTestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ServiceTestConfig.class)
public abstract class AdminModuleIntegrationTest extends IntegrationTestSupport {

    /**
     * 컨텍스트 캐시 공유를 위해 베이스에서 mock으로 고정한다. 개별 테스트 클래스에 @MockitoBean을 선언하면
     * 클래스마다 새 컨텍스트가 생성된다. 실제 ReportRateLimitStore 동작은 ReportRateLimitStoreTest(ServiceTest 컨텍스트)가 검증한다.
     */
    @MockitoBean
    protected ReportRateLimitStore rateLimitStore;
}
