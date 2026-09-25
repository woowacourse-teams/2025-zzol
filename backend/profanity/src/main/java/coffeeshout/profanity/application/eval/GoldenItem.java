package coffeeshout.profanity.application.eval;

import java.util.List;

/**
 * 정답을 붙인 닉네임 한 건.
 *
 * @param expectedTerms 자동 차단 때 뽑혀야 할 비속어 조각. PROFANE·EVASION에서만 의미가 있다.
 * @param category      리포트를 나눠 볼 묶음 이름. 직접욕설, 자모분리처럼 우회 수법이나 정상어 유형을 적는다.
 */
public record GoldenItem(String nickname, Expected expected, List<String> expectedTerms, String category) {

    public GoldenItem {
        expectedTerms = List.copyOf(expectedTerms);
    }

    public enum Expected {
        /** 그대로 드러난 욕설. */
        PROFANE,
        /** 자모 분리, 특수문자 삽입처럼 우회한 욕설. */
        EVASION,
        /** 사람이 봐도 갈리는 닉네임. 사람 확인(PENDING)으로 보내는 게 정답이다. */
        AMBIGUOUS,
        CLEAN;

        public boolean isPositive() {
            return this == PROFANE || this == EVASION;
        }
    }
}
