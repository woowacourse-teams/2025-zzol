package coffeeshout.admin.user.domain;

/**
 * 소셜 제공자별 연결 수.
 *
 * <p><b>회원 수가 아니라 연결 수다.</b> 한 사람이 구글과 카카오를 모두 연결할 수 있어서
 * 셋을 다 더해도 회원 수와 맞지 않는다. 화면에서 이 사실을 밝히지 않으면 합이 안 맞는
 * 숫자로 보여 지표 전체를 못 믿게 된다.
 *
 * @param provider {@code OAuthProvider.getRegistrationId()} 그대로다. 소문자 {@code google},
 *                 {@code kakao}, {@code naver}. 한글 이름은 화면이 붙인다
 * @param count    그 제공자로 연결된 계정 수. 탈퇴 회원은 빠진다
 */
public record ProviderCount(String provider, long count) {}
