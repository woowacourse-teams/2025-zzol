package coffeeshout.admin.support;

import coffeeshout.AdminModuleIntegrationTest;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.domain.AdminTokenIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 관리자 API 를 실제 필터 체인을 거쳐 호출하는 테스트의 바탕.
 *
 * <p>컨트롤러 단위 테스트가 못 보는 것을 본다. 보안 체인이 이 경로를 잡는지, 매핑과 파라미터
 * 검증이 붙는지, 예외가 어떤 상태 코드로 나가는지, 조치가 감사 로그에 남는지. 그 층은
 * 목으로 컨트롤러를 직접 부르면 통째로 건너뛴다.
 *
 * <p>토큰을 <b>진짜로 발급해서</b> 헤더에 붙인다. {@code @WithMockUser} 로 인증을 심지 않는
 * 이유는 그러면 JWT 필터가 안 도는데, 그 필터가 principal 을 채우는 방식이 감사 로그의
 * 담당자 이름을 결정하기 때문이다. 인증을 흉내 내면 그 연결이 테스트에서 사라진다.
 */
public abstract class AdminApiE2eTest extends AdminModuleIntegrationTest {

    protected static final String ACTOR = "admin@zzol.site";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private AdminTokenIssuer adminTokenIssuer;

    /** 관리자로 인증된 요청. */
    protected RequestPostProcessor admin() {
        return admin(ACTOR);
    }

    protected RequestPostProcessor admin(String email) {
        final String token = adminTokenIssuer.issue(AdminEmail.of(email));
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
