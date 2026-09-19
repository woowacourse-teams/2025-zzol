package coffeeshout.zzolbot.monitor.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CitationGrammarTest {

    @Nested
    class 로그_줄만_인용하게_만든다 {

        @Test
        void 로그_줄이_리터럴로_들어간다() {
            final String grammar = CitationGrammar.forLogSamples(List.of("[ERROR] 방 참가 실패"));

            assertThat(grammar).contains("line0 ::= \"[ERROR] 방 참가 실패\"");
        }

        @Test
        void 중복된_줄은_한_번만_들어간다() {
            final String grammar = CitationGrammar.forLogSamples(List.of("같은 줄", "같은 줄", "다른 줄"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(grammar).contains("anyline ::= line0 | line1");
                softly.assertThat(grammar).doesNotContain("line2 ::=");
            });
        }

        @Test
        void 빈_줄과_null은_버린다() {
            final String grammar = CitationGrammar.forLogSamples(Arrays.asList("실재 로그", null, "   "));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(grammar).contains("line0 ::= \"실재 로그\"");
                softly.assertThat(grammar).doesNotContain("line1 ::=");
            });
        }
    }

    @Nested
    class 이스케이프가_두_겹으로_겹친다 {

        @Test
        void 따옴표가_두_겹으로_이스케이프된다() {
            // JSON 본문에서 따옴표는 역슬래시와 함께 두 글자이고, 문법 리터럴 안에서 그 역슬래시를 또 escape 한다
            final String grammar = CitationGrammar.forLogSamples(List.of("message=\"down\""));

            assertThat(grammar).contains("line0 ::= \"message=\\\\\\\"down\\\\\\\"\"");
        }

        @Test
        void 역슬래시가_이스케이프된다() {
            final String grammar = CitationGrammar.forLogSamples(List.of("C:\\logs\\app.log"));

            assertThat(grammar).contains("line0 ::= \"C:\\\\\\\\logs\\\\\\\\app.log\"");
        }
    }

    @Nested
    class 사고로_배운_계약을_고정한다 {

        @Test
        void 빈_인용을_허용한다() {
            // 막으면 근거 없는 알림에서 모델이 억지로 로그를 쓰다 JSON을 닫지 못한다
            assertThat(CitationGrammar.forLogSamples(List.of("a"))).contains("cite ::= \"\\\"\\\"\" |");
        }

        @Test
        void 로그가_하나도_없으면_빈_인용만_허용한다() {
            final String grammar = CitationGrammar.forLogSamples(List.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(grammar).contains("cite ::= \"\\\"\\\"\"");
                softly.assertThat(grammar).doesNotContain("anyline");
            });
        }

        @Test
        void 인용이_닫히면_객체를_닫는_것만_남는다() {
            // 안 막으면 필드를 다시 쓰는 반복 생성에 빠진다
            assertThat(CitationGrammar.forLogSamples(List.of("a")))
                    .contains("\"\\\"evidenceLine\\\":\" ws cite ws \"}\"");
        }

        @Test
        void 모든_규칙이_한_줄에_있다() {
            // 괄호 밖에서는 줄바꿈이 규칙의 끝이다. 여러 줄에 걸치면 문법 파싱이 실패한다
            final String grammar = CitationGrammar.forLogSamples(List.of("[ERROR] a", "[ERROR] b"));

            assertThat(grammar.lines().filter(line -> !line.isBlank()))
                    .allSatisfy(line -> assertThat(line).contains("::="));
        }
    }
}
