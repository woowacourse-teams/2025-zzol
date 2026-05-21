package coffeeshout.friend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "friend.presence")
public record FriendPresenceProperties(
        @Positive long gracePeriodSeconds
) {
}
