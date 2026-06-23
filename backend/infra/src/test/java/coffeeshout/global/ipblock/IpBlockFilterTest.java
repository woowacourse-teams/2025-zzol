package coffeeshout.global.ipblock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("IpBlockFilter")
@ExtendWith(MockitoExtension.class)
class IpBlockFilterTest {

    private static final Ip REMOTE_IP = new Ip("1.2.3.4");
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
            given(ipBlockStore.isBlocked(any(Ip.class))).willReturn(false);
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
    class IP_추출 {

        @Test
        void RemoteAddr를_IP로_사용한다() throws Exception {
            given(ipBlockStore.isBlocked(REMOTE_IP)).willReturn(true);

            filter.doFilter(요청(REMOTE_IP, NORMAL_PATH), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should().isBlocked(REMOTE_IP);
        }

        @Test
        void 유효하지_않은_IP_형식이면_차단_검사_없이_filterChain을_통과한다() throws Exception {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("not-an-ip");
            request.setRequestURI(NORMAL_PATH);

            filter.doFilter(request, new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).shouldHaveNoInteractions();
            then(filterChain).should().doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("내부 IP 화이트리스트")
    class 내부_IP_화이트리스트 {

        @ParameterizedTest
        @ValueSource(strings = {
                "127.0.0.1",    // 루프백
                "10.0.0.1",     // RFC1918 (10/8)
                "172.21.0.5",   // RFC1918 (172.16/12) — postmortem 0003 사고 IP
                "192.168.0.1",  // RFC1918 (192.168/16)
                "169.254.0.1",  // 링크로컬
                "100.64.0.1",   // CGNAT (RFC6598)
                "fd00::1",      // IPv6 ULA (RFC4193)
        })
        void 내부_IP는_차단_검사와_카운트_없이_통과한다(String internalIp) throws Exception {
            filter.doFilter(요청(new Ip(internalIp), NORMAL_PATH), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).shouldHaveNoInteractions();
            then(filterChain).should().doFilter(any(), any());
        }

        @Test
        void 내부_IP는_404_응답에도_누적_카운트하지_않는다() throws Exception {
            doAnswer(invocation -> {
                ((HttpServletResponse) invocation.getArgument(1)).setStatus(404);
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilter(요청(new Ip("172.21.0.5"), "/not-found"), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should(never()).incrementNotFoundAndBlockIfExceeded(any());
        }

        @Test
        void 내부_IP의_악성_경로_접근은_차단하지_않고_통과한다() throws Exception {
            given(maliciousPathMatcher.isMalicious(MALICIOUS_PATH)).willReturn(true);

            filter.doFilter(요청(new Ip("172.21.0.5"), MALICIOUS_PATH), new MockHttpServletResponse(), filterChain);

            then(ipBlockStore).should(never()).blockImmediately(any());
            then(filterChain).should().doFilter(any(), any());
        }
    }

    private MockHttpServletRequest 요청(Ip remoteAddr, String uri) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr.value());
        request.setRequestURI(uri);
        return request;
    }
}
