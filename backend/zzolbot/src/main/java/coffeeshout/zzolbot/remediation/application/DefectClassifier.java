package coffeeshout.zzolbot.remediation.application;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 모니터링 실행의 근본원인 가설·신호를 보고 결함 유형을 추정한다. 키워드 매칭 기반의 보수적 분류기로,
 * 확신이 없으면 {@link DefectType#UNKNOWN}으로 떨어뜨려 자동 수정을 시도하지 않는다(틀린 PR을 막는 1차 게이트).
 *
 * <p>LLM에 분류를 또 맡기지 않는 이유: 분류는 디스패치 여부를 가르는 안전 게이트라 결정적이고 감사 가능해야
 * 하고, 이미 근본원인 가설을 만든 LLM 출력을 그대로 신뢰하면 게이트가 LLM 환각에 종속된다.
 */
@Component
public class DefectClassifier {

    // NPE 계열 + 빈 Optional 역참조(orElseThrow/get → NoSuchElementException) 신호.
    private static final List<String> NULL_POINTER_KEYWORDS = List.of(
            "nullpointerexception",
            "nosuchelementexception",
            "orelsethrow",
            "optional.get",
            "널 포인터",
            "null 역참조",
            "널 역참조");

    // 짧은 약어 "npe"는 다른 단어 내부 우연 포함을 피해 단어 경계로만 매칭한다(오탐 방지).
    private static final Pattern NPE_ABBREVIATION = Pattern.compile("\\bnpe\\b");

    public DefectType classify(String rootCauseHypothesis, String signalsJson) {
        final String haystack = (safe(rootCauseHypothesis) + " " + safe(signalsJson)).toLowerCase(Locale.ROOT);
        if (NULL_POINTER_KEYWORDS.stream().anyMatch(haystack::contains) || NPE_ABBREVIATION.matcher(haystack).find()) {
            return DefectType.NULL_POINTER;
        }
        return DefectType.UNKNOWN;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
