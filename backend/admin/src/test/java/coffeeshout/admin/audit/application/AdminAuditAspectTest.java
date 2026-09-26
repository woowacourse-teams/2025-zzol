package coffeeshout.admin.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.audit.domain.AdminAuditResult;
import coffeeshout.admin.auth.domain.AdminPrincipal;
import coffeeshout.admin.ipblock.ui.AdminIpBlockController;
import coffeeshout.patchnote.ui.AdminPatchNoteController;
import coffeeshout.report.ui.AdminReportController;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

@DisplayName("AdminAuditAspect")
@ExtendWith(MockitoExtension.class)
class AdminAuditAspectTest {

    @Mock
    private AdminAuditLogService adminAuditLogService;

    @InjectMocks
    private AdminAuditAspect aspect;

    @Captor
    private ArgumentCaptor<String> actorCaptor;

    @Captor
    private ArgumentCaptor<String> actionCaptor;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    private void bindRequest(String method, String uri, String pattern, Map<String, String> vars) {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (pattern != null) {
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
        }
        if (vars != null) {
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, vars);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        new AdminPrincipal(AdminEmail.of(email)),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    /** 원래 메서드가 값을 돌려주는 조인포인트. */
    private static org.aspectj.lang.ProceedingJoinPoint returning(Object value) {
        return new StubJoinPoint(() -> value);
    }

    /** 원래 메서드가 예외를 던지는 조인포인트. */
    private static org.aspectj.lang.ProceedingJoinPoint throwing(Throwable error) {
        return new StubJoinPoint(() -> {
            throw error;
        });
    }

    /**
     * 포인트컷이 어느 컨트롤러까지 보는지.
     *
     * <p>어드바이스 본문만 테스트하면 이 층이 통째로 빈다. 실제로 {@code within(coffeeshout.admin..*)}
     * 가 걸려 있어서 신고, 패치노트, ZzolBot 의 조치가 감사 로그에 남지 않았는데, 본문
     * 테스트는 전부 통과하고 있었다. 대상을 고르는 식을 <b>애너테이션에서 직접 읽어</b> 건다.
     */
    @Nested
    class 대상_선정 {

        /**
         * 메서드 단위로 본다. {@code @within} 의 클래스 필터는 넉넉하게 참을 돌려줄 때가
         * 있어서, 클래스 필터만 보면 잡지 않는 것도 잡는다고 나온다.
         */
        private static boolean advises(Class<?> type) throws NoSuchMethodException {
            final Around around = AdminAuditAspect.class
                    .getMethod("recordWrite", ProceedingJoinPoint.class)
                    .getAnnotation(Around.class);
            final AspectJExpressionPointcut expression = new AspectJExpressionPointcut();
            expression.setExpression(around.value());

            return Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .anyMatch(method -> expression.matches(method, type));
        }

        @ParameterizedTest
        @ValueSource(
                classes = {AdminIpBlockController.class, AdminReportController.class, AdminPatchNoteController.class})
        void 관리자_API_컨트롤러는_패키지와_무관하게_잡는다(Class<?> controller) throws Exception {
            // 뒤의 둘은 coffeeshout.admin 밖에 있다. 무엇을 남길지는 패키지가 아니라 주소가
            // 정한다. 패키지로 고르면 새 컨트롤러가 다른 곳에 생길 때 조용히 빠진다.
            //
            // ZzolBot 컨트롤러 셋도 같은 처지이지만 여기 넣지 못한다. :admin 이 :zzolbot 을
            // 의존하지 않아 타입이 안 보인다. 모듈 의존 방향을 테스트 하나 때문에 뒤집지
            // 않는다. 같은 패키지 밖이라는 사실은 위 둘이 이미 고정한다.
            assertThat(advises(controller)).isTrue();
        }

        @Test
        void 컨트롤러가_아니면_잡지_않는다() throws Exception {
            assertThat(advises(AdminAuditLogService.class)).isFalse();
        }
    }

    @Nested
    class 기록하는_경우 {

        @Test
        void 쓰기_요청을_성공으로_기록한다() throws Throwable {
            bindRequest("POST", "/admin/api/accounts", "/admin/api/accounts", null);
            authenticateAs("mj@zzol.site");

            aspect.recordWrite(returning("ok"));

            then(adminAuditLogService)
                    .should()
                    .record(
                            actorCaptor.capture(),
                            actionCaptor.capture(),
                            any(),
                            any(),
                            any(),
                            org.mockito.ArgumentMatchers.eq(AdminAuditResult.SUCCESS));
            assertThat(actorCaptor.getValue()).isEqualTo("mj@zzol.site");
            assertThat(actionCaptor.getValue()).isEqualTo("POST /admin/api/accounts");
        }

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
        void 상태를_바꾸는_모든_메서드를_기록한다(String method) throws Throwable {
            bindRequest(method, "/admin/api/accounts", "/admin/api/accounts", null);
            authenticateAs("mj@zzol.site");

            aspect.recordWrite(returning("ok"));

            then(adminAuditLogService).should().record(any(), any(), any(), any(), any(), any());
        }

        @Test
        void 실제_id가_아니라_매핑_패턴을_action에_남긴다() throws Throwable {
            // id마다 다른 action이 쌓이면 "무슨 조치가 몇 번 있었나"를 집계할 수 없다.
            bindRequest("DELETE", "/admin/api/accounts/7", "/admin/api/accounts/{id}", Map.of("id", "7"));
            authenticateAs("mj@zzol.site");

            aspect.recordWrite(returning(null));

            then(adminAuditLogService).should().record(any(), actionCaptor.capture(), any(), any(), any(), any());
            assertThat(actionCaptor.getValue()).isEqualTo("DELETE /admin/api/accounts/{id}");
        }

        @Test
        void 매핑_패턴이_없으면_요청_URI_대신_핸들러_이름을_남긴다() throws Throwable {
            // 요청 URI 는 부르는 쪽이 정하는 문자열이다. 개행을 끼워 넣으면 감사 로그 한 줄이
            // 여러 줄로 쪼개지고, 같은 값이 조치 이력 테이블에도 그대로 쌓인다.
            bindRequest("POST", "/admin/api/accounts\n가짜 로그 줄", null, null);
            authenticateAs("mj@zzol.site");

            aspect.recordWrite(returning("ok"));

            then(adminAuditLogService).should().record(any(), actionCaptor.capture(), any(), any(), any(), any());
            assertThat(actionCaptor.getValue()).doesNotContain("가짜 로그 줄").isEqualTo("POST stub");
        }

        @Test
        void 자원_이름과_대상_id를_함께_남긴다() throws Throwable {
            bindRequest("DELETE", "/admin/api/accounts/7", "/admin/api/accounts/{id}", Map.of("id", "7"));
            authenticateAs("mj@zzol.site");

            aspect.recordWrite(returning(null));

            final ArgumentCaptor<String> targetType = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<String> targetId = ArgumentCaptor.forClass(String.class);
            then(adminAuditLogService)
                    .should()
                    .record(any(), any(), targetType.capture(), targetId.capture(), any(), any());
            assertThat(targetType.getValue()).isEqualTo("accounts");
            assertThat(targetId.getValue()).isEqualTo("7");
        }

        @Test
        void 인증_전_로그인_시도는_anonymous로_기록한다() throws Throwable {
            bindRequest("POST", "/admin/api/auth/login", "/admin/api/auth/login", null);

            aspect.recordWrite(returning("token"));

            then(adminAuditLogService).should().record(actorCaptor.capture(), any(), any(), any(), any(), any());
            assertThat(actorCaptor.getValue()).isEqualTo("anonymous");
        }

        @Test
        void 예외가_나면_실패로_기록하고_예외를_그대로_던진다() {
            bindRequest("POST", "/admin/api/accounts", "/admin/api/accounts", null);
            authenticateAs("mj@zzol.site");
            final IllegalStateException error = new IllegalStateException("이미 등록된 관리자");

            assertThatThrownBy(() -> aspect.recordWrite(throwing(error))).isSameAs(error);

            final ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
            then(adminAuditLogService)
                    .should()
                    .record(
                            any(),
                            any(),
                            any(),
                            any(),
                            detail.capture(),
                            org.mockito.ArgumentMatchers.eq(AdminAuditResult.FAILURE));
            assertThat(detail.getValue()).contains("IllegalStateException", "이미 등록된 관리자");
        }
    }

    @Nested
    class 기록하지_않는_경우 {

        @Test
        void 조회는_기록하지_않는다() throws Throwable {
            // 목록을 열어본 기록까지 쌓으면 실제 조치가 묻힌다.
            bindRequest("GET", "/admin/api/accounts", "/admin/api/accounts", null);
            authenticateAs("mj@zzol.site");

            assertThat(aspect.recordWrite(returning("list"))).isEqualTo("list");
            then(adminAuditLogService).should(never()).record(any(), any(), any(), any(), any(), any());
        }

        @Test
        void 토큰_재발급은_기록하지_않는다() throws Throwable {
            // 조치가 아니라 세션 유지다. 남기면 탭마다 한 시간에 한 줄씩 anonymous 기록이 쌓인다.
            bindRequest("POST", "/admin/api/auth/refresh", "/admin/api/auth/refresh", null);

            assertThat(aspect.recordWrite(returning("token"))).isEqualTo("token");
            then(adminAuditLogService).should(never()).record(any(), any(), any(), any(), any(), any());
        }

        @Test
        void admin_api_밖의_경로는_기록하지_않는다() throws Throwable {
            bindRequest("POST", "/reports", "/reports", null);

            aspect.recordWrite(returning("ok"));

            then(adminAuditLogService).should(never()).record(any(), any(), any(), any(), any(), any());
        }

        @Test
        void 요청_컨텍스트가_없으면_기록하지_않는다() throws Throwable {
            // 스케줄러나 이벤트 리스너에서 호출되는 경우다.
            assertThat(aspect.recordWrite(returning("ok"))).isEqualTo("ok");
            then(adminAuditLogService).should(never()).record(any(), any(), any(), any(), any(), any());
        }
    }

    /** ProceedingJoinPoint 는 인터페이스가 넓어 필요한 proceed()만 구현한 스텁을 쓴다. */
    private static class StubJoinPoint implements org.aspectj.lang.ProceedingJoinPoint {

        private interface Action {
            Object run() throws Throwable;
        }

        private final Action action;

        StubJoinPoint(Action action) {
            this.action = action;
        }

        @Override
        public Object proceed() throws Throwable {
            return action.run();
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            return action.run();
        }

        @Override
        public void set$AroundClosure(org.aspectj.runtime.internal.AroundClosure arc) {}

        @Override
        public String toShortString() {
            return "stub";
        }

        @Override
        public String toLongString() {
            return "stub";
        }

        @Override
        public Object getThis() {
            return null;
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public Object[] getArgs() {
            return new Object[0];
        }

        @Override
        public org.aspectj.lang.Signature getSignature() {
            return null;
        }

        @Override
        public org.aspectj.lang.reflect.SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public String getKind() {
            return "method-execution";
        }

        @Override
        public org.aspectj.lang.JoinPoint.StaticPart getStaticPart() {
            return null;
        }
    }
}
