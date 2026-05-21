package coffeeshout.room.infra.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "websocket.room-session-token")
public record RoomSessionTokenProperties(
        @NotBlank
        @Size(min = 32, message = "Room Session Token secret은 HS256 최소 키 길이(32자) 이상이어야 합니다.")
        @Pattern(regexp = "^[\\x20-\\x7E]+$", message = "Room Session Token secret은 ASCII 문자만 허용됩니다.")
        String secret
) {
}
