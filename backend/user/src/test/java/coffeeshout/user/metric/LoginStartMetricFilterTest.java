package coffeeshout.user.metric;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginStartMetricFilterTest {

    private MeterRegistry meterRegistry;
    private LoginStartMetricFilter filter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        filter = new LoginStartMetricFilter(new LoginMetrics(meterRegistry));
    }

    @Test
    void 인가_시작_경로는_provider별로_센다() throws Exception {
        doFilter("/oauth2/authorization/kakao");
        doFilter("/oauth2/authorization/kakao");
        doFilter("/oauth2/authorization/google");

        assertThat(count("kakao")).isEqualTo(2.0);
        assertThat(count("google")).isEqualTo(1.0);
    }

    @Test
    void 인가_시작이_아닌_경로는_세지_않는다() throws Exception {
        doFilter("/login/oauth2/code/kakao");
        doFilter("/api/rooms");

        assertThat(meterRegistry.find("login.start").counter()).isNull();
    }

    @Test
    void provider_하위경로가_있으면_세지_않는다() throws Exception {
        doFilter("/oauth2/authorization/kakao/extra");

        assertThat(meterRegistry.find("login.start").counter()).isNull();
    }

    @Test
    void 지원하지_않는_provider는_세지_않는다() throws Exception {
        // 무검증 태그로 인한 카디널리티 폭발 방지
        doFilter("/oauth2/authorization/evil-random-value");

        assertThat(meterRegistry.find("login.start").counter()).isNull();
    }

    @Test
    void provider_대소문자는_정규화되어_집계된다() throws Exception {
        doFilter("/oauth2/authorization/KAKAO");
        doFilter("/oauth2/authorization/kakao");

        // 성공 카운트 태그(소문자 enum 값)와 동일한 값집합으로 정규화된다
        assertThat(count("kakao")).isEqualTo(2.0);
        assertThat(meterRegistry.find("login.start").tag("provider", "KAKAO").counter()).isNull();
    }

    @Test
    void 경로와_무관하게_다음_필터로_체인을_이어간다() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    private void doFilter(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private double count(String provider) {
        return meterRegistry.find("login.start").tag("provider", provider).counter().count();
    }
}
