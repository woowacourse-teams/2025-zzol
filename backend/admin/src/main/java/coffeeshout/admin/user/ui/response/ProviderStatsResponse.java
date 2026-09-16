package coffeeshout.admin.user.ui.response;

import coffeeshout.admin.user.domain.ProviderCount;
import java.util.List;

/**
 * 소셜 제공자 분포.
 *
 * <p>{@code userCount} 를 함께 준다. 제공자별 연결 수를 다 더해도 회원 수와 맞지 않기
 * 때문이다. 한 사람이 구글과 카카오를 모두 연결할 수 있다. 화면에 회원 수가 없으면
 * 운영자는 연결 수의 합을 회원 수로 읽고, 다른 화면의 회원 수와 어긋나는 것을 보고
 * 지표 전체를 의심하게 된다.
 *
 * @param userCount 활성 회원 수. 탈퇴 회원은 빠진다
 * @param providers 연결이 있는 제공자만. 많은 순
 */
public record ProviderStatsResponse(long userCount, List<Provider> providers) {

    public static ProviderStatsResponse of(long userCount, List<ProviderCount> counts) {
        return new ProviderStatsResponse(
                userCount,
                counts.stream()
                        .map(count -> new Provider(count.provider(), count.count()))
                        .toList());
    }

    /**
     * @param provider 소문자 {@code google}, {@code kakao}, {@code naver}. 한글 이름은 화면이 붙인다
     */
    public record Provider(String provider, long count) {}
}
