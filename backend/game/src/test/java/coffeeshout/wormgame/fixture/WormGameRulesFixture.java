package coffeeshout.wormgame.fixture;

import coffeeshout.wormgame.config.WormGameRulesProperties;
import coffeeshout.wormgame.domain.WormGameRules;
import java.util.Map;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * 기본 규칙의 단일 출처는 {@link WormGameRulesProperties}의 {@code @DefaultValue}다.
 * 값을 여기에 다시 적으면 사본이 하나 더 생겨 운영과 조용히 갈라진다 — 빈 소스로 바인딩해 그대로 읽어온다.
 */
public final class WormGameRulesFixture {

    private WormGameRulesFixture() {}

    public static WormGameRulesProperties defaultProperties() {
        return new Binder(new MapConfigurationPropertySource(Map.of()))
                .bindOrCreate("worm-game.rules", WormGameRulesProperties.class);
    }

    public static WormGameRules defaultRules() {
        return defaultProperties().toRules();
    }
}
