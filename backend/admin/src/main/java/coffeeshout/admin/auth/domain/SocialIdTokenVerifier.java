package coffeeshout.admin.auth.domain;

/**
 * 소셜 제공자가 발급한 ID 토큰을 검증한다.
 *
 * <p>인터페이스로 둔 이유는 테스트 때문이다. 구현체는 구글 JWKS를 네트워크로 가져오므로
 * 서비스 계층 테스트가 외부 호출에 묶이면 안 된다.
 */
public interface SocialIdTokenVerifier {

    /**
     * @return 검증에 성공한 계정의 이메일 (정규화 이전 원본)
     * @throws coffeeshout.global.exception.custom.BusinessException 서명, 발급자, 대상, 이메일 검증 중 하나라도 실패한 경우
     */
    String verifyAndExtractEmail(String idToken);
}
