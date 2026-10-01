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

    /**
     * 첫 틱에 아레나가 스폰 지점 안쪽까지 좁혀지는 규칙 — 전원이 경계 밖에서 즉사해 라운드가 바로 끝난다.
     * 경계 사망은 무적 구간에도 적용되므로 무적 틱을 건드릴 필요가 없다. 기본 규칙으로 종료를 기다리면
     * 아레나가 자연히 좁아질 때까지 수십 초가 걸려 IT에 쓸 수 없다.
     */
    public static WormGameRules 첫_틱에_전멸하는_규칙() {
        return new Binder(new MapConfigurationPropertySource(Map.of(
                        "worm-game.rules.shrink-delay-ticks", "0",
                        "worm-game.rules.shrink-duration-ticks", "1",
                        "worm-game.rules.shrink-min-ratio", "0.01")))
                .bindOrCreate("worm-game.rules", WormGameRulesProperties.class)
                .toRules();
    }
}
