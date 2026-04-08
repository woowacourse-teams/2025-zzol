package coffeeshout.global.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;

import coffeeshout.global.ratelimit.IpBlockStore;
import coffeeshout.global.ratelimit.MaliciousPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("IpBlockFilter")
@ExtendWith(MockitoExtension.class)
class IpBlockFilterTest {

    private static final String REMOTE_IP = "1.2.3.4";
    private static final String MALICIOUS_PATH = "/.env";
    private static final String NORMAL_PATH = "/reports";

    @Mock
    private IpBlockStore ipBlockStore;

    @Mock
    private MaliciousPathMatcher maliciousPathMatcher;

    @Mock
    private FilterChain filterChain;

    private IpBlockFilter filter;

    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new IpBlockFilter(ipBlockStore, maliciousPathMatcher, objectMapper);
    }

    @Nested
    class 차단된_IP_접근 {

        @Test
        void 차단된_IP는_429를_반환하고_filterChain을_통과하지_않는다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            final MockHttpServletRequest request = 요청(REMOTE_IP, NORMAL_PATH);
            final MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).isEqualTo(429);
                softly.assertThat(response.getContentType()).contains("application/json");
            });
            then(filterChain).shouldHaveNoInteractions();
        }
    }

    @Nested
    class 악성_경로_접근 {

        @BeforeEach
        void setUp() {
            given(ipBlockStore.isBlocked(anyString())).willReturn(false);
            given(maliciousPathMatcher.isMalicious(MALICIOUS_PATH)).willReturn(true);
        }

        @Test
        void 악성_경로_접근_시_429를_반환하고_IP를_즉시_차단한다() throws Exception {
            final MockHttpServletRequest request = 요청(REMOTE_IP, MALICIOUS_PATH);
            final MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).isEqualTo(429);
                softly.assertThat(response.getContentType()).contains("application/json");
            });
            then(ipBlockStore).should().blockImmediately(REMOTE_IP);
            then(filterChain).shouldHaveNoInteractions();
        }
    }

    @Nested
    class 정상_요청의_404_누적 {

        @BeforeEach
        void setUp() {
            given(ipBlockStore.isBlocked(anyString())).willReturn(false);
            given(maliciousPathMatcher.isMalicious(anyString())).willReturn(false);
        }

        @Test
        void _404_응답_시_카운터를_증가시킨다() throws Exception {
            doAnswer(invocation -> {
                ((HttpServletResponse) invocation.getArgument(1)).setStatus(404);
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilter(요청(REMOTE_IP, "/not-found"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().incrementNotFoundAndBlockIfExceeded(REMOTE_IP);
        }

        @Test
        void _200_응답_시_카운터를_증가시키지_않는다() throws Exception {
            filter.doFilter(요청(REMOTE_IP, NORMAL_PATH), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should(never()).incrementNotFoundAndBlockIfExceeded(any());
        }
    }

    @Nested
    class IP_추출 {

        @Test
        void X_Forwarded_For_헤더의_마지막_IP를_사용한다() throws Exception {
            given(ipBlockStore.isBlocked(anyString())).willReturn(true);

            final MockHttpServletRequest request = 요청("172.16.0.1", NORMAL_PATH);
            request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");

            filter.doFilter(request, new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().isBlocked("192.168.1.1");
        }

        @Test
        void X_Forwarded_For_헤더가_없으면_RemoteAddr를_사용한다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            filter.doFilter(요청(REMOTE_IP, NORMAL_PATH), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().isBlocked(REMOTE_IP);
        }

        @Test
        void 헤더_엔트리_사이의_공백을_제거하고_마지막_IP를_사용한다() throws Exception {
            given(ipBlockStore.isBlocked(anyString())).willReturn(true);

            final MockHttpServletRequest request = 요청("172.16.0.1", NORMAL_PATH);
            request.addHeader("X-Forwarded-For", "  10.0.0.1 ,  192.168.1.1  ");

            filter.doFilter(request, new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().isBlocked("192.168.1.1");
        }

        @Test
        void X_Forwarded_For_헤더가_단일_IP이면_해당_IP를_사용한다() throws Exception {
            given(ipBlockStore.isBlocked("10.0.0.1")).willReturn(true);

            final MockHttpServletRequest request = 요청("172.16.0.1", NORMAL_PATH);
            request.addHeader("X-Forwarded-For", "10.0.0.1");

            filter.doFilter(request, new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().isBlocked("10.0.0.1");
        }

        @Test
        void X_Forwarded_For_헤더가_빈_문자열이면_RemoteAddr를_사용한다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            final MockHttpServletRequest request = 요청(REMOTE_IP, NORMAL_PATH);
            request.addHeader("X-Forwarded-For", "");

            filter.doFilter(request, new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().isBlocked(REMOTE_IP);
        }
    }

    private MockHttpServletRequest 요청(String remoteAddr, String uri) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        request.setRequestURI(uri);
        return request;
    }
}
