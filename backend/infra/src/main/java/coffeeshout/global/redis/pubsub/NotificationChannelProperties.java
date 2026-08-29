package coffeeshout.global.redis.pubsub;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 알림 채널 pub/sub 어댑터 설정.
 * <p>
 * 기본값을 코드에 두는 이유는 테스트 컨텍스트 보호다 — 테스트 설정(application-test-base.yml)은 운영
 * {@code redis.yml}을 미러링하는 구조라, 새 prefix를 운영 yml에만 추가하면 테스트에서 채널명이 null로
 * 바인딩된다. 운영 실효값은 {@code redis.yml}에 명시하므로 설정 소유권은 yml에 남는다
 * ({@code RedisStreamProperties.CommonSettings}의 {@code @DefaultValue} 선례).
 */
@Validated
@ConfigurationProperties(prefix = "redis.notification")
public record NotificationChannelProperties(
        @DefaultValue("notification:ws") @NotBlank String channel
) {
}
