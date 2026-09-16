package coffeeshout.profanity.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * @param minInterval 비속어 트라이 재빌드 최소 간격(#1759). 신호를 병합해도 신호가 계속 들어오면 재빌드가
 *                     쉬지 않고 붙어 돈다. 한 번 돌고 나면 이 간격 안에는 다시 돌지 않는다.
 *                     기본값 5초의 근거는 {@code ProfanityTrieRebuildScheduler}가 이미 매시 정각
 *                     전량 재빌드를 안전망으로 돌려 최대 1시간 지연을 설계로 받아들이고 있다는 것이다.
 *                     몇 초 지연은 그 안에 넉넉히 든다. 0이면 간격 없이 병합만 동작하며, 측정 비교에 쓴다.
 */
@Validated
@ConfigurationProperties(prefix = "profanity.trie.rebuild")
public record ProfanityTrieRebuildProperties(
        @NotNull @DurationMin(nanos = 0, message = "최소 간격은 0 이상이어야 합니다") @DefaultValue("5s")
        Duration minInterval) {}
