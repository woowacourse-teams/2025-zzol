package coffeeshout.profanity.domain.audit;

import java.util.List;
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
