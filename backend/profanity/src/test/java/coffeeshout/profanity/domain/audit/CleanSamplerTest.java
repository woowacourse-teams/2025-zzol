package coffeeshout.profanity.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class CleanSamplerTest {

    @Test
    void CLEAN만_뽑고_표시한다() {
        final CleanSampler sampler = new CleanSampler(1.0, 20);
        final NicknameAudit clean = judged("용감한호랑이", NicknameAuditStatus.CLEAN);
        final NicknameAudit flagged = judged("욕설닉네임", NicknameAuditStatus.FLAGGED);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(sampler.trySample(clean)).isTrue();
            softly.assertThat(clean.isReviewSample()).isTrue();
            softly.assertThat(sampler.trySample(flagged)).isFalse();
            softly.assertThat(flagged.isReviewSample()).isFalse();
        });
    }

    @Test
    void 비율이_0이면_아무것도_뽑지_않는다() {
        final CleanSampler sampler = new CleanSampler(0, 20);

        assertThat(sampler.trySample(judged("용감한호랑이", NicknameAuditStatus.CLEAN)))
                .isFalse();
    }

    @Test
    void 상한만큼만_뽑는다() {
        final CleanSampler sampler = new CleanSampler(1.0, 2);

        final List<Boolean> sampled = IntStream.range(0, 3)
                .mapToObj(i -> sampler.trySample(judged("닉" + i, NicknameAuditStatus.CLEAN)))
                .toList();

        assertThat(sampled).containsExactly(true, true, false);
    }

    /** 벌크 저장이 실패해 건별 폴백이 같은 엔티티를 다시 넘겨도 예산을 한 번만 쓴다. */
    @Test
    void 이미_뽑힌_행은_다시_세지_않는다() {
        final CleanSampler sampler = new CleanSampler(1.0, 2);
        final NicknameAudit clean = judged("용감한호랑이", NicknameAuditStatus.CLEAN);

        sampler.trySample(clean);
        final boolean again = sampler.trySample(clean);
        final boolean next = sampler.trySample(judged("다른닉네임", NicknameAuditStatus.CLEAN));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(again).isFalse();
            softly.assertThat(next).as("예산이 하나 남아 있어야 한다.").isTrue();
        });
    }

    @Test
    void 같은_닉네임은_늘_같은_결과가_나오고_비율에_가깝게_뽑힌다() {
        final List<String> nicknames =
                IntStream.range(0, 2_000).mapToObj(i -> "닉" + i).toList();

        final List<String> firstRun = sampled(nicknames);
        final List<String> secondRun = sampled(nicknames);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(secondRun).isEqualTo(firstRun);
            softly.assertThat(firstRun).hasSizeBetween(60, 140);
        });
    }

    private static List<String> sampled(List<String> nicknames) {
        final CleanSampler sampler = new CleanSampler(0.05, Integer.MAX_VALUE);
        return nicknames.stream()
                .filter(nickname -> sampler.trySample(judged(nickname, NicknameAuditStatus.CLEAN)))
                .toList();
    }

    private static NicknameAudit judged(String nickname, NicknameAuditStatus status) {
        final NicknameAudit audit = new NicknameAudit(nickname);
        audit.complete(status, AiConfidence.of(0.99), "사유");
        return audit;
    }
}
