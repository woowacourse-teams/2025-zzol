package coffeeshout.zzolbot.monitor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LogSampleNormalizerTest {

    @Nested
    class 제어문자를_없앤다 {

        @Test
        void 탭을_공백으로_바꾼다() {
            // 모델이 이 줄을 JSON 문자열에 인용하면 파서가 raw 제어문자를 거부한다
            final List<String> result =
                    LogSampleNormalizer.normalize(List.of("ERROR OOM\tat com.foo.Bar(Bar.java:42)"));

            assertThat(result.get(0)).doesNotContain("\t").contains("ERROR OOM");
        }

        @Test
        void 개행도_공백으로_바꾼다() {
            final List<String> result = LogSampleNormalizer.normalize(List.of("ERROR 실패\n원인: 커넥션 없음"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.get(0)).doesNotContain("\n");
                softly.assertThat(result.get(0)).contains("ERROR 실패").contains("커넥션 없음");
            });
        }
    }

    @Nested
    class 스택_프레임을_접는다 {

        @Test
        void 앞_세_개만_남기고_나머지는_생략_표시한다() {
            final String line = "ERROR 실패"
                    + " at com.a.A(A.java:1)"
                    + " at com.b.B(B.java:2)"
                    + " at com.c.C(C.java:3)"
                    + " at com.d.D(D.java:4)"
                    + " at com.e.E(E.java:5)";

            final String result = LogSampleNormalizer.normalize(List.of(line)).get(0);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).contains("at com.c.C(C.java:3)");
                softly.assertThat(result).doesNotContain("com.d.D").doesNotContain("com.e.E");
                softly.assertThat(result).contains("(스택 2줄 생략)");
            });
        }

        @Test
        void 세_개_이하면_그대로_둔다() {
            final String line = "ERROR 실패 at com.a.A(A.java:1) at com.b.B(B.java:2)";

            assertThat(LogSampleNormalizer.normalize(List.of(line)).get(0)).isEqualTo(line);
        }
    }

    @Nested
    class 길이를_제한한다 {

        @Test
        void 팔백자를_넘으면_자르고_표시한다() {
            final String line = "E".repeat(1000);

            final String result = LogSampleNormalizer.normalize(List.of(line)).get(0);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSizeLessThan(line.length());
                softly.assertThat(result).endsWith("…(잘림)");
            });
        }

        @Test
        void 짧은_줄은_손대지_않는다() {
            final String line = "[2026-09-21 23:41:37.214] [ERROR] c.web.exception.RestExceptionHandler : method=POST";

            assertThat(LogSampleNormalizer.normalize(List.of(line)).get(0)).isEqualTo(line);
        }
    }

    @Nested
    class 비어_있는_입력 {

        @Test
        void null이면_빈_목록을_준다() {
            assertThat(LogSampleNormalizer.normalize(null)).isEmpty();
        }

        @Test
        void null과_공백_줄은_버린다() {
            final List<String> result = LogSampleNormalizer.normalize(Arrays.asList("실재 로그", null, "   "));

            assertThat(result).containsExactly("실재 로그");
        }
    }

    @Nested
    class 골든셋_형태는_그대로_통과한다 {

        @Test
        void 탭도_긴_스택도_없는_줄은_한_글자도_바뀌지_않는다() {
            // 골든셋 33종 140건이 전부 이 형태다(탭 0건, 800자 초과 0건, 스택 3개 초과 0건).
            // 이 테스트가 깨지면 기존 측정값의 전제가 무너진다.
            final List<String> samples = List.of(
                    "2026-07-22 14:03:11 ERROR [nunchi-consumer] 처리 실패 joinCode=ABCD reason=timeout",
                    "2026-07-22 14:03:12 ERROR [room-join] 정원 초과 joinCode=EFGH");

            assertThat(LogSampleNormalizer.normalize(samples)).isEqualTo(samples);
        }
    }
}
