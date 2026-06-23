package coffeeshout.zzolbot.monitor.config;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalWebhookTokenFilterTest {

    private static final String TOKEN = "s3cr3t-token";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Test
    void 유효한_베어러_토큰이면_체인을_진행한다() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Bearer " + TOKEN);

        new InternalWebhookTokenFilter(TOKEN).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void 토큰이_틀리면_401로_차단한다() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Bearer wrong");

        new InternalWebhookTokenFilter(TOKEN).doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void Authorization_헤더가_없으면_401로_차단한다() throws Exception {
        given(request.getHeader("Authorization")).willReturn(null);

        new InternalWebhookTokenFilter(TOKEN).doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void 설정_토큰이_비어있으면_유효한_형식이어도_거부한다() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Bearer anything");

        new InternalWebhookTokenFilter("").doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }
}
