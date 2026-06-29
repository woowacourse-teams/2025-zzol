package coffeeshout.profanity.domain.audit;

import coffeeshout.profanity.domain.TextNormalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record NicknameAuditResult(
        String nickname,
        NicknameAuditStatus status,
        AiConfidence confidence,
        String reason,
        List<String> profanityTerms
) {

    public NicknameAuditResult {
        profanityTerms = (profanityTerms == null)
                ? List.of()
                : profanityTerms.stream().filter(Objects::nonNull).toList();
    }

    /**
     * AI가 추출한 비속어 조각 중 차단 대상으로 채택할 유효 조각만 골라낸다.
     * 원본 닉네임의 (정규화 기준) 부분 문자열이어야 하고, 정규화 후 길이가 {@code minTermLength}
     * 미만인 조각은 Trie 부분일치 특성상 과차단을 유발하므로 제외하며, 정규화 결과가 같은 중복 조각은
     * 한 번만 채택한다. 등록은 raw 형태로 하므로 정규화 키별 첫 raw 조각을 반환한다.
     * 유효한 조각이 하나도 없으면 빈 리스트를 반환한다(폴백 정책은 호출자가 결정한다).
     */
    public List<String> extractProfanityFragments(TextNormalizer textNormalizer, int minTermLength) {
        final String normalizedNickname = textNormalizer.normalize(nickname);
        final Map<String, String> validByNormalized = new LinkedHashMap<>();
        for (String term : profanityTerms) {
            if (term == null || term.isBlank()) {
                continue;
            }
            final String normalizedTerm = textNormalizer.normalize(term);
            if (normalizedTerm.length() < minTermLength || !normalizedNickname.contains(normalizedTerm)) {
                continue;
            }
            validByNormalized.putIfAbsent(normalizedTerm, term);
        }
        return List.copyOf(validByNormalized.values());
    }

    public NicknameAuditResult(String nickname, NicknameAuditStatus status, AiConfidence confidence, String reason) {
        this(nickname, status, confidence, reason, List.of());
    }

    public static NicknameAuditResult of(
            String nickname, boolean flagged, double confidence, String reason, double flaggedThreshold
    ) {
        return of(nickname, flagged, confidence, reason, List.of(), flaggedThreshold);
    }

    public static NicknameAuditResult of(
            String nickname, boolean flagged, double confidence, String reason,
            List<String> profanityTerms, double flaggedThreshold
    ) {
        final AiConfidence aiConfidence = AiConfidence.of(confidence);

        if (!flagged) {
            return new NicknameAuditResult(nickname, NicknameAuditStatus.CLEAN, aiConfidence, reason, List.of());
        }
        if (aiConfidence.value().doubleValue() >= flaggedThreshold) {
            return new NicknameAuditResult(nickname, NicknameAuditStatus.FLAGGED, aiConfidence, reason, profanityTerms);
        }
        return new NicknameAuditResult(nickname, NicknameAuditStatus.PENDING, aiConfidence, reason, List.of());
    }
}
