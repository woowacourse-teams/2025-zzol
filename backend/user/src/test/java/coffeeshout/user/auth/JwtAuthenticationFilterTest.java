package coffeeshout.user.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.UserErrorCode;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private AuthTokenService authTokenService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        authTokenService = mock(AuthTokenService.class);
        filter = new JwtAuthenticationFilter(authTokenService);
        SecurityContextHolder.clearContext();
    }

    @Nested
    class Authorization_헤더가_없을_때 {

        @Test
        void SecurityContext가_비어있고_요청이_통과된다() throws Exception {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final MockHttpServletResponse response = new MockHttpServletResponse();
            final FilterChain chain = mock(FilterChain.class);

            filter.doFilterInternal(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    class Bearer_토큰이_유효할_때 {

        @Test
        void SecurityContext에_AuthenticatedUser가_설정된다() throws Exception {
            final AuthenticatedUser user = new AuthenticatedUser(1L, "ABCDF");
            given(authTokenService.verify("valid-token")).willReturn(user);

            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer valid-token");
            final MockHttpServletResponse response = new MockHttpServletResponse();
            final FilterChain chain = mock(FilterChain.class);

            filter.doFilterInternal(request, response, chain);

            final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal).isEqualTo(user);
        }
    }

    @Nested
    class Bearer_토큰이_유효하지_않을_때 {

        @Test
        void SecurityContext가_비어있고_요청이_통과된다() throws Exception {
            given(authTokenService.verify("invalid-token"))
                    .willThrow(new BusinessException(UserErrorCode.INVALID_TOKEN, "위변조된 토큰"));

            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer invalid-token");
            final MockHttpServletResponse response = new MockHttpServletResponse();
            final FilterChain chain = mock(FilterChain.class);

            filter.doFilterInternal(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void 만료된_토큰도_SecurityContext가_비어있고_요청이_통과된다() throws Exception {
            given(authTokenService.verify("expired-token"))
                    .willThrow(new BusinessException(UserErrorCode.TOKEN_EXPIRED, "만료된 토큰"));

            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer expired-token");
            final MockHttpServletResponse response = new MockHttpServletResponse();
            final FilterChain chain = mock(FilterChain.class);

            filter.doFilterInternal(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
