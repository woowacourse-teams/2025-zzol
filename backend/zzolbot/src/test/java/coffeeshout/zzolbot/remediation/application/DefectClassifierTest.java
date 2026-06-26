package coffeeshout.zzolbot.remediation.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import org.junit.jupiter.api.Test;

class DefectClassifierTest {

    private final DefectClassifier classifier = new DefectClassifier();

    @Test
    void NPE_가설은_NULL_POINTER로_분류한다() {
        assertThat(classifier.classify("NullPointerException으로 orderId 역참조 실패", "{}"))
                .isEqualTo(DefectType.NULL_POINTER);
    }

    @Test
    void 빈_Optional_orElseThrow_신호도_NULL_POINTER로_분류한다() {
        assertThat(classifier.classify("", "{\"summary\":\"room.orElseThrow 에서 NoSuchElementException\"}"))
                .isEqualTo(DefectType.NULL_POINTER);
    }

    @Test
    void 무관한_가설은_UNKNOWN으로_분류한다() {
        assertThat(classifier.classify("HikariCP 커넥션 풀 고갈로 타임아웃", "{\"alertname\":\"DbPool\"}"))
                .isEqualTo(DefectType.UNKNOWN);
    }

    @Test
    void null_입력은_UNKNOWN으로_분류한다() {
        assertThat(classifier.classify(null, null)).isEqualTo(DefectType.UNKNOWN);
    }
}
