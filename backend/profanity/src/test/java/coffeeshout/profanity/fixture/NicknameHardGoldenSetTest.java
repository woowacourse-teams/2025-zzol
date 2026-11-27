package coffeeshout.profanity.fixture;

import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.기본_세트;
import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.하드_세트;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.eval.GoldenItem;
import coffeeshout.profanity.eval.GoldenItem.Expected;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 하드 골든셋 CSV가 러너에 넣을 수 있는 형태인지 본다. Gemini를 부르지 않아 기본 test에서 돈다.
 */
class NicknameHardGoldenSetTest {

    // UserNickname.MAX_LENGTH, PlayerName.MAX_NAME_LENGTH와 같은 값이다. :profanity는 두 모듈에 의존하지 않아 옮겨 적는다
    private static final int 닉네임_최대_길이 = 10;
    private static final int 조각_최소_길이 =
            NicknameAuditPropertiesFixture.API_키("api-key").minTermLength();
    private static final Set<String> 축 =
            Set.of("자모분리", "한영오타", "발음변형", "숫자섞기", "닮은꼴문자", "초성숫자혼합", "복합삽입", "성적표현", "혐오차별", "집단비하", "하드네거티브", "애매");

    private final TextNormalizer normalizer = new TextNormalizer();
    // expected 값이 허용 집합 밖이면 Expected.valueOf가 여기서 던져 모든 테스트가 실패한다
    private final List<GoldenItem> 하드셋 = NicknameGoldenSetFixture.골든셋(하드_세트);

    @Test
    void 건수가_목표_범위_안이다() {
        assertThat(하드셋).hasSizeBetween(250, 350);
    }

    @Test
    void 모든_닉네임이_길이_제한_안이다() {
        assertThat(하드셋)
                .allSatisfy(item -> assertThat(item.nickname()).isNotBlank().hasSizeLessThanOrEqualTo(닉네임_최대_길이));
    }

    @Test
    void 파일_안에_정규화_중복이_없다() {
        assertThat(하드셋)
                .extracting(item -> normalizer.normalize(item.nickname()))
                .doesNotHaveDuplicates();
    }

    @Test
    void 기본_골든셋과_원문도_정규화_결과도_겹치지_않는다() {
        final List<GoldenItem> 기본셋 = NicknameGoldenSetFixture.골든셋(기본_세트);
        final Set<String> 기본_원문 = 기본셋.stream().map(GoldenItem::nickname).collect(Collectors.toSet());
        final Set<String> 기본_정규화 =
                기본셋.stream().map(item -> normalizer.normalize(item.nickname())).collect(Collectors.toSet());

        assertThat(하드셋).allSatisfy(item -> {
            assertThat(기본_원문).doesNotContain(item.nickname());
            assertThat(기본_정규화).doesNotContain(normalizer.normalize(item.nickname()));
        });
    }

    @Test
    void category는_정해_둔_축_이름만_쓴다() {
        assertThat(하드셋).extracting(GoldenItem::category).isSubsetOf(축);
    }

    @Test
    void 하드네거티브가_전체의_30퍼센트_이상이다() {
        final long 하드네거티브 =
                하드셋.stream().filter(item -> item.category().equals("하드네거티브")).count();

        assertThat(하드네거티브 * 10).isGreaterThanOrEqualTo(하드셋.size() * 3L);
        assertThat(하드셋.stream().filter(item -> item.category().equals("하드네거티브")))
                .allSatisfy(item -> assertThat(item.expected()).isEqualTo(Expected.CLEAN));
    }

    @Test
    void 정답_조각은_차단_대상에만_있고_닉네임_안에_들어_있다() {
        assertThat(하드셋).allSatisfy(item -> {
            if (!item.expected().isPositive()) {
                assertThat(item.expectedTerms()).isEmpty();
                return;
            }
            // 정답 조각이 채택 규칙을 못 통과하면 조각 정확도가 늘 틀린다
            assertThat(item.expectedTerms()).isNotEmpty().allSatisfy(term -> {
                final String normalized = normalizer.normalize(term);
                assertThat(normalized).hasSizeGreaterThanOrEqualTo(조각_최소_길이);
                assertThat(normalizer.normalize(item.nickname())).contains(normalized);
            });
        });
    }

    @Test
    void 환경변수가_비어_있으면_기본_세트를_고른다() {
        assertThat(NicknameGoldenSetFixture.세트_이름(null)).isEqualTo(기본_세트);
        assertThat(NicknameGoldenSetFixture.세트_이름(" ")).isEqualTo(기본_세트);
        assertThat(NicknameGoldenSetFixture.세트_이름(" hard ")).isEqualTo(하드_세트);
    }

    @Test
    void 모르는_세트_이름은_거절한다() {
        assertThatThrownBy(() -> NicknameGoldenSetFixture.골든셋("없는세트")).isInstanceOf(IllegalArgumentException.class);
    }
}
