package coffeeshout.wormgame.config;

import coffeeshout.wormgame.domain.WormGameRules;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 물리·판정 튜닝 노브. 기본값은 설계 문서 v0.3 파라미터 표이고, {@code worm-game.rules.*}로 덮어쓴다.
 * {@code tick-millis}는 {@code worm-game.timing.tick}과 같아야 점수(생존 ms)가 실시간과 일치한다.
 */
@Validated
@ConfigurationProperties(prefix = "worm-game.rules")
public record WormGameRulesProperties(
        @DefaultValue("50") @Positive long tickMillis,
        @DefaultValue("120") @Positive double baseSpeed,
        @DefaultValue("1.6") @Positive double maxSpeedMultiplier,
        @DefaultValue("1200") @Positive int speedRampTicks,
        @DefaultValue("200") @Positive double omegaBaseDegrees,
        @DefaultValue("0.7") @PositiveOrZero double omegaExponent,
        @DefaultValue("220") @Positive double arenaBaseRadius,
        @DefaultValue("0.5") @Positive double arenaExponent,
        @DefaultValue("200") @PositiveOrZero int shrinkDelayTicks,
        @DefaultValue("1200") @Positive int shrinkDurationTicks,
        @DefaultValue("0.30") @Positive double shrinkMinRatio,
        @DefaultValue("0.05") @PositiveOrZero double lateShrinkPerTick,
        @DefaultValue("40") @Positive double minRadius,
        @DefaultValue("40") @PositiveOrZero int invincibleTicks,
        @DefaultValue("6") @Positive double trailRadius,
        @DefaultValue("3") @PositiveOrZero int wetPaintSkipSegments) {

    public WormGameRules toRules() {
        return new WormGameRules(
                tickMillis,
                baseSpeed,
                maxSpeedMultiplier,
                speedRampTicks,
                Math.toRadians(omegaBaseDegrees),
                omegaExponent,
                arenaBaseRadius,
                arenaExponent,
                shrinkDelayTicks,
                shrinkDurationTicks,
                shrinkMinRatio,
                lateShrinkPerTick,
                minRadius,
                invincibleTicks,
                trailRadius,
                wetPaintSkipSegments);
    }
}
