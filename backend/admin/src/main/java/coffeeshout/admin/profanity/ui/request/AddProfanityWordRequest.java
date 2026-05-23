package coffeeshout.admin.profanity.ui.request;

import coffeeshout.profanity.domain.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddProfanityWordRequest(
        @NotBlank @Size(max = 200) String word,
        @NotNull Language language
) {
}
