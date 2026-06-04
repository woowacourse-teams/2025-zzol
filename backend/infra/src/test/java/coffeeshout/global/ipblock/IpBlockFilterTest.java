package coffeeshout.global.ipblock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
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
    private static final String NORMAL_PATH = "/api/game";

    @Mock
    private IpBlockStore ipBlockStore;

    @Mock
    private MaliciousPathMatcher maliciousPathMatcher;

    @Mock
    private IpBlockProperties properties;

    @Mock
    private FilterChain filterChain;

    private IpBlockFilter filter;

    @BeforeEach
    void setUp() {
        // lenient: 악성 경로 테스트에서는 isMalicious 체크 직후 return되어 exemptPaths에 도달하지 않음
        lenient().when(properties.exemptPaths()).thenReturn(List.of("/admin", "/reports"));
        lenient().when(properties.notFoundExemptPaths()).thenReturn(List.of("/ws"));
        final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new IpBlockFilter(ipBlockStore, maliciousPathMatcher, properties, objectMapper);
    }

    @Nested
    class 차단된_IP_접근 {

        @Test
        void 차단된_IP는_403을_반환하고_filterChain을_통과하지_않는다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            final MockHttpServletRequest request = 요청(REMOTE_IP, NORMAL_PATH);
            final MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).isEqualTo(403);
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
                softly.assertThat(response.getStatus()).isEqualTo(403);
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

        @Test
        void 악성_경로_접근_시_403을_반환하고_IP를_즉시_차단한다() throws Exception {
            given(maliciousPathMatcher.isMalicious(MALICIOUS_PATH)).willReturn(true);

            final MockHttpServletRequest request = 요청(REMOTE_IP, MALICIOUS_PATH);
            final MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).isEqualTo(403);
                softly.assertThat(response.getContentType()).contains("application/json");
            });
            then(ipBlockStore).should().blockImmediately(REMOTE_IP);
            then(filterChain).shouldHaveNoInteractions();
        }

        @Test
        void 악성_경로는_예외_경로_prefix여도_즉시_차단한다() throws Exception {
            given(maliciousPathMatcher.isMalicious("/admin.php")).willReturn(true);

            final MockHttpServletRequest request = 요청(REMOTE_IP, "/admin.php");
            final MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).isEqualTo(403);
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

        @Test
        void WS_엔드포인트의_404_응답은_카운터를_증가시키지_않는다() throws Exception {
            doAnswer(invocation -> {
                ((HttpServletResponse) invocation.getArgument(1)).setStatus(404);
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilter(요청(REMOTE_IP, "/ws/abc/123/websocket"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should(never()).incrementNotFoundAndBlockIfExceeded(any());
        }
    }

    @Nested
    class 예외_경로 {

        @Test
        void admin_경로는_차단된_IP도_filterChain을_통과한다() throws Exception {
            filter.doFilter(요청(REMOTE_IP, "/admin/ip-blocks"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).shouldHaveNoInteractions();
            then(filterChain).should().doFilter(any(), any());
        }

        @Test
        void admin_하위_경로도_filterChain을_통과한다() throws Exception {
            filter.doFilter(요청(REMOTE_IP, "/admin/reports/1/resolve"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).shouldHaveNoInteractions();
        }

        @Test
        void reports_경로는_차단된_IP도_filterChain을_통과한다() throws Exception {
            filter.doFilter(요청(REMOTE_IP, "/reports"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).shouldHaveNoInteractions();
            then(filterChain).should().doFilter(any(), any());
        }

        @Test
        void adminXX_같은_유사_경로는_예외_처리되지_않는다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            filter.doFilter(요청(REMOTE_IP, "/administrator"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().isBlocked(REMOTE_IP);
        }

        @Test
        void 예외_경로가_아닌_일반_경로는_차단_검사를_수행한다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            filter.doFilter(요청(REMOTE_IP, NORMAL_PATH), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().isBlocked(REMOTE_IP);
        }
    }

    @Nested
    class 사설_내부_IP {

        private static final String INTERNAL_IP = "172.20.0.5";

        @Test
        void 차단_등록된_사설_IP도_filterChain을_통과한다() throws Exception {
            lenient().when(maliciousPathMatcher.isMalicious(anyString())).thenReturn(false);
            lenient().when(ipBlockStore.isBlocked(INTERNAL_IP)).thenReturn(true);

            final MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(요청(INTERNAL_IP, NORMAL_PATH), response, filterChain);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).isNotEqualTo(403);
            });
            then(filterChain).should().doFilter(any(), any());
        }

        @Test
        void 사설_IP의_404_응답은_카운터를_증가시키지_않는다() throws Exception {
            lenient().when(maliciousPathMatcher.isMalicious(anyString())).thenReturn(false);
            lenient().when(ipBlockStore.isBlocked(anyString())).thenReturn(false);
            doAnswer(invocation -> {
                ((HttpServletResponse) invocation.getArgument(1)).setStatus(404);
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilter(요청(INTERNAL_IP, "/not-found"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should(never()).incrementNotFoundAndBlockIfExceeded(any());
        }

        @Test
        void 사설_IP가_악성_경로_접근_시_차단하지_않고_경고만_기록한다() throws Exception {
            given(maliciousPathMatcher.isMalicious(MALICIOUS_PATH)).willReturn(true);

            filter.doFilter(요청(INTERNAL_IP, MALICIOUS_PATH), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should(never()).blockImmediately(any());
            then(ipBlockStore).should().recordInternalIpSuspicious(INTERNAL_IP, MALICIOUS_PATH);
            then(filterChain).should().doFilter(any(), any());
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
