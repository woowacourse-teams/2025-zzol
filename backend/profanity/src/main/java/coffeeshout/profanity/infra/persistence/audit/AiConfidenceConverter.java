package coffeeshout.profanity.infra.persistence.audit;

import coffeeshout.profanity.domain.audit.AiConfidence;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class AiConfidenceConverter implements AttributeConverter<AiConfidence, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(AiConfidence attribute) {
        return attribute != null ? attribute.value() : null;
    }

    @Override
    public AiConfidence convertToEntityAttribute(BigDecimal dbData) {
        return dbData != null ? new AiConfidence(dbData) : null;
    }
}
