package coffeeshout.zzolbot.monitor.infra;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 인용 필드를 실제 로그 줄로만 생성하도록 강제하는 GBNF 문법을 만든다.
 *
 * <p>소형 모델의 인용 실패 대부분은 판단이 아니라 전사 오류다. 옳은 줄을 고르고 밀리초 두 자리를
 * 틀린다. 문법으로 막으면 없는 로그를 쓰고 싶어도 쓸 수 없다.
 *
 * <p>강제하는 것이 셋이다.
 * <ul>
 *   <li>{@code evidenceLine}은 주어진 로그 줄 그대로이거나 <b>빈 문자열</b>이다. 빈 문자열을 막으면
 *       근거가 없는 알림에서 모델이 억지로 로그를 쓰다 JSON을 닫지 못한다</li>
 *   <li>인용이 닫히면 객체를 닫는 것 말고 할 수 있는 것이 없다. 안 그러면 필드를 다시 쓰는 반복
 *       생성에 빠진다</li>
 *   <li>응답 스키마 전체를 강제하므로 형식 실패가 구조적으로 사라진다</li>
 * </ul>
 *
 * <p><b>규칙은 한 줄에 하나씩 쓴다.</b> 괄호 밖에서는 줄바꿈이 규칙의 끝이라 보기 좋게 쪼개면
 * 문법 파싱이 실패한다.
 */
public final class CitationGrammar {

    /** 요약과 원인 가설의 최대 길이. 시스템 지시가 요구하는 1~2문장에 넉넉하다. */
    private static final int MAX_TEXT_CHARS = 200;

    /** 제안 조치 한 건의 최대 길이. */
    private static final int MAX_ACTION_CHARS = 150;

    /** 제안 조치 개수 상한. 운영자가 실제로 읽는 수를 넘지 않는다. */
    private static final int MAX_ACTIONS = 3;

    private static final String JSON_CHAR =
            "[^\"\\\\\\x00-\\x1F] | \"\\\\\" [\"\\\\/bfnrt] | \"\\\\u\" [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F]";

    private CitationGrammar() {}

    public static String forLogSamples(List<String> logSamples) {
        final Set<String> bodies = new LinkedHashSet<>();
        if (logSamples != null) {
            for (String line : logSamples) {
                if (line != null && !line.isBlank()) {
                    bodies.add(jsonBody(line));
                }
            }
        }
        final List<String> unique = List.copyOf(bodies);
        final StringBuilder sb = new StringBuilder();
        sb.append("root ::= \"{\" ws \"\\\"summary\\\":\" ws str \",\" ws ")
                .append("\"\\\"rootCauseHypothesis\\\":\" ws str \",\" ws ")
                .append("\"\\\"suggestedActions\\\":\" ws arr \",\" ws ")
                .append("\"\\\"evidenceFound\\\":\" ws bool \",\" ws ")
                .append("\"\\\"evidenceLine\\\":\" ws cite ws \"}\"\n");
        if (unique.isEmpty()) {
            // 로그가 없으면 인용할 것도 없다. 빈 문자열만 허용한다
            sb.append("cite ::= \"\\\"\\\"\"\n");
        } else {
            sb.append("cite ::= \"\\\"\\\"\" | \"\\\"\" anyline \"\\\"\"\n");
            sb.append("anyline ::= ")
                    .append(java.util.stream.IntStream.range(0, unique.size())
                            .mapToObj(i -> "line" + i)
                            .collect(Collectors.joining(" | ")))
                    .append('\n');
            for (int i = 0; i < unique.size(); i++) {
                sb.append("line")
                        .append(i)
                        .append(" ::= ")
                        .append(literal(unique.get(i)))
                        .append('\n');
            }
        }
        // 길이를 문법으로 묶는다. 안 묶으면 출력이 원리적으로 무한해 토큰 상한에서 잘리고,
        // 잘린 JSON 은 닫히지 않아 통째로 버려진다. 실제로 정확히 상한(700 토큰)에서 잘렸다(#1815).
        sb.append("str ::= \"\\\"\" char{0,").append(MAX_TEXT_CHARS).append("} \"\\\"\"\n");
        sb.append("astr ::= \"\\\"\" char{0,").append(MAX_ACTION_CHARS).append("} \"\\\"\"\n");
        sb.append("char ::= ").append(JSON_CHAR).append('\n');
        sb.append("arr ::= \"[\" ws (astr (ws \",\" ws astr){0,")
                .append(MAX_ACTIONS - 1)
                .append("})? ws \"]\"\n");
        sb.append("bool ::= \"true\" | \"false\"\n");
        sb.append("ws ::= [ \\t\\n]*\n");
        return sb.toString();
    }

    /** JSON 문자열 본문으로 만든다. 바깥 따옴표는 뗀다. */
    private static String jsonBody(String line) {
        return new String(JsonStringEncoder.getInstance().quoteAsString(line));
    }

    /**
     * GBNF 리터럴로 감싼다. 이스케이프가 두 겹 겹치는 자리다. 들어오는 값은 이미 JSON 본문이라
     * 따옴표가 역슬래시와 함께 두 글자로 들어 있고, 그 역슬래시를 문법 리터럴 안에서 또 escape 한다.
     */
    private static String literal(String jsonBody) {
        return '"' + jsonBody.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
