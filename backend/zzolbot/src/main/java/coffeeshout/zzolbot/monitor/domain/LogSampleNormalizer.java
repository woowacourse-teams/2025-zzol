package coffeeshout.zzolbot.monitor.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM에 넘기기 전에 ERROR 로그 샘플을 다듬는다. 두 모델과 인용 검증, 문법 생성이 모두 이 결과를 쓴다.
 *
 * <p><b>한 곳에서 한 번만 다듬는다.</b> 모델마다 다르게 자르면 두 모델의 차이가 모델에서 온 것인지
 * 입력에서 온 것인지 가릴 수 없다. 인용 검증도 여기서 나온 문자열을 기준으로 해야 모델이 옮겨 적은
 * 줄과 대조 대상이 같아진다.
 *
 * <p>규칙이 셋이고 각각 실제로 겪은 문제에서 나왔다.
 *
 * <ul>
 *   <li><b>제어문자를 공백으로.</b> 스택 트레이스를 한 줄로 펴면 {@code \tat ...}의 탭이 남는다.
 *       모델이 그 줄을 JSON 문자열에 인용하면 파서가 raw 제어문자를 거부해 분석이 통째로 버려진다</li>
 *   <li><b>스택 프레임은 앞 몇 개만.</b> 나머지는 부피만 차지한다. 예외 메시지와 발생 위치가 근거지
 *       호출 경로 전체가 근거인 경우는 없다</li>
 *   <li><b>샘플 하나의 길이 상한.</b> 실제 알림에서 프롬프트가 13,364 토큰까지 커져 자체 모델의
 *       컨텍스트를 넘겼다. 골든셋 최대가 2,574 토큰이라 예상보다 5배였다</li>
 * </ul>
 */
public final class LogSampleNormalizer {

    private static final int MAX_STACK_FRAMES = 3;
    private static final int MAX_SAMPLE_LENGTH = 800;
    private static final String TRUNCATED_MARK = " …(잘림)";

    /** 한 줄로 펴진 스택 프레임. {@code at pkg.Class.method(File.java:42)} 형태를 잡는다. */
    private static final Pattern STACK_FRAME = Pattern.compile("\\s+at\\s+[\\w$.]+\\([^)]*\\)");

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}]+");

    private LogSampleNormalizer() {}

    public static List<String> normalize(List<String> logSamples) {
        if (logSamples == null || logSamples.isEmpty()) {
            return List.of();
        }
        final List<String> normalized = new ArrayList<>(logSamples.size());
        for (String line : logSamples) {
            if (line == null || line.isBlank()) {
                continue;
            }
            normalized.add(normalizeLine(line));
        }
        return List.copyOf(normalized);
    }

    private static String normalizeLine(String line) {
        final String noControl = CONTROL_CHARS.matcher(line).replaceAll(" ");
        return truncate(foldStackFrames(noControl));
    }

    /**
     * 스택 프레임을 앞 {@value #MAX_STACK_FRAMES}개만 남기고 나머지는 생략 표시로 접는다.
     * 몇 줄을 접었는지 적어 둬야 사람이 로그를 다시 찾아볼 때 잘린 것을 안다.
     */
    private static String foldStackFrames(String line) {
        final Matcher matcher = STACK_FRAME.matcher(line);
        final StringBuilder sb = new StringBuilder();
        int kept = 0;
        int folded = 0;
        int cursor = 0;
        while (matcher.find()) {
            kept++;
            if (kept <= MAX_STACK_FRAMES) {
                continue;
            }
            if (folded == 0) {
                sb.append(line, 0, matcher.start());
                cursor = matcher.start();
            }
            folded++;
            cursor = matcher.end();
        }
        if (folded == 0) {
            return line;
        }
        sb.append(" ... (스택 ").append(folded).append("줄 생략)").append(line.substring(cursor));
        return sb.toString();
    }

    private static String truncate(String line) {
        if (line.length() <= MAX_SAMPLE_LENGTH) {
            return line;
        }
        return line.substring(0, MAX_SAMPLE_LENGTH) + TRUNCATED_MARK;
    }
}
