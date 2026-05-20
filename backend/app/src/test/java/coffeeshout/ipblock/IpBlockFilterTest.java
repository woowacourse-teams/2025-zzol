package coffeeshout.ipblock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;

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

        @Test
        void 차단된_IP_접근_시_Origin_헤더가_있으면_CORS_헤더를_추가한다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            final String origin = "https://example.com";
            final MockHttpServletRequest request = 요청(REMOTE_IP, NORMAL_PATH);
            request.addHeader("Origin", origin);
            final MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).isEqualTo(429);
                softly.assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
                softly.assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
            });
        }

        @Test
        void 차단된_IP_접근_시_OPTIONS_메서드면_204를_반환한다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            final MockHttpServletRequest request = 요청(REMOTE_IP, NORMAL_PATH);
            request.setMethod("OPTIONS");
            request.addHeader("Origin", "https://example.com");
            final MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).isEqualTo(204);
                softly.assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://example.com");
            });
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
        void 미등록_경로의_404_응답_시_카운터를_증가시킨다() throws Exception {
            doAnswer(invocation -> {
                ((HttpServletResponse) invocation.getArgument(1)).setStatus(404);
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilter(요청(REMOTE_IP, "/not-found"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().incrementNotFoundAndBlockIfExceeded(REMOTE_IP);
        }

        @Test
        void 비즈니스_예외_속성이_설정된_404_응답_시_카운터를_증가시키지_않는다() throws Exception {
            final MockHttpServletRequest request = 요청(REMOTE_IP, "/users/999");
            request.setAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND, true);

            doAnswer(invocation -> {
                ((HttpServletResponse) invocation.getArgument(1)).setStatus(404);
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilter(request, new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should(never()).incrementNotFoundAndBlockIfExceeded(any());
        }

        @Test
        void 비즈니스_예외_속성이_설정된_404_응답이_반복되어도_카운터를_증가시키지_않는다() throws Exception {
            doAnswer(invocation -> {
                ((HttpServletResponse) invocation.getArgument(1)).setStatus(404);
                return null;
            }).when(filterChain).doFilter(any(), any());

            for (int i = 0; i < 10; i++) {
                final MockHttpServletRequest request = 요청(REMOTE_IP, "/users/999");
                request.setAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND, true);
                filter.doFilter(request, new MockHttpServletResponse(), filterChain);
            }

            then(ipBlockStore).should(never()).incrementNotFoundAndBlockIfExceeded(any());
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
        void RemoteAddr를_IP로_사용한다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            filter.doFilter(요청(REMOTE_IP, NORMAL_PATH), new MockHttpServletResponse(), filterChain);

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
