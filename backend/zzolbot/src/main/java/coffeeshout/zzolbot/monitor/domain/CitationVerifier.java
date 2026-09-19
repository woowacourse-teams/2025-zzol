package coffeeshout.zzolbot.monitor.domain;

import java.util.List;

/**
 * 모델이 근거로 인용한 로그 줄이 실제 로그 샘플에 존재하는지 확인한다. 지어낸 이벤트명·수치를 근거로
 * 내세우는 실패를 코드로 차단한다.
 *
 * <p>스택 트레이스처럼 로그 한 건이 여러 줄일 수 있으므로, 개행·연속 공백을 단일 공백으로 접은 뒤
 * 비교해 인용의 줄바꿈·들여쓰기 차이는 무시한다.
 *
 * <p><b>분석기 바깥으로 뺀 이유.</b> 판정 규칙이 한 분석기 안에 있으면 다른 분석기는 같은 잣대를 쓸 수
 * 없다. 두 모델의 답을 비교하려면 강등 규칙이 한 벌이어야 하고, 규칙이 두 벌이면 차이가 모델에서 온
 * 것인지 채점에서 온 것인지 가릴 수 없다.
 */
public final class CitationVerifier {

    private CitationVerifier() {}

    // ponytail: 공백 정규화 후 부분 문자열 포함만 본다. 의미 매칭·토큰 단위 매칭이 필요하면 그때 올린다.
    public static boolean citedInLogs(String evidenceLine, List<String> logSamples) {
        if (evidenceLine == null || evidenceLine.isBlank() || logSamples == null) {
            return false;
        }
        final String needle = normalizeWhitespace(evidenceLine);
        return logSamples.stream().filter(line -> line != null).anyMatch(line -> normalizeWhitespace(line)
                .contains(needle));
    }

    private static String normalizeWhitespace(String text) {
        return text.strip().replaceAll("\\s+", " ");
    }
}
