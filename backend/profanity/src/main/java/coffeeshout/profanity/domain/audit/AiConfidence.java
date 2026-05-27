package coffeeshout.profanity.domain.audit;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AiConfidence(BigDecimal value) {

    public static final AiConfidence UNKNOWN = new AiConfidence(BigDecimal.ZERO);

    public AiConfidence {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidence는 0.0~1.0 사이여야 합니다. 입력값: " + value);
        }
        value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static AiConfidence of(double rawValue) {
        return new AiConfidence(BigDecimal.valueOf(rawValue));
    }
}
