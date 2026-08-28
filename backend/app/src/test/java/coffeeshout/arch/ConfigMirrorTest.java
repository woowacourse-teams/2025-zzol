package coffeeshout.arch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Redis Stream 키·게임 timing 프로퍼티를 프로덕션에만 추가하고 테스트 설정 미러를 빠뜨리는 사고를 막는다.
 * 빠지면 "메시지 미수신" 타임아웃(스트림)이나 {@code @Validated} 바인딩 실패(timing)로 나타나 원인을 찾기 어렵다.
 *
 * <p>네 파일 모두 테스트 클래스패스 리소스라 파일 경로 계산이 필요 없고, Spring 컨텍스트도 Docker도 띄우지 않는다.
 */
class ConfigMirrorTest {

    private static final String PRODUCTION_REDIS = "config/redis.yml";
    private static final String TEST_REDIS = "application-test-base.yml";
    private static final String PRODUCTION_GAME = "config/game.yml";
    private static final String TEST_GAME = "application-test-game.yml";

    /** 운영과 테스트가 의도적으로 다른 값 — 테스트 부하 기준으로 큐를 짧게 잡는다(운영 1024 / 테스트 128). */
    private static final Set<String> INTENTIONALLY_DIFFERENT = Set.of("queue-capacity");

    @Test
    void redis_스트림_키셋이_운영과_테스트에서_같다() {
        assertThat(streams(TEST_REDIS).keySet())
                .as("테스트 설정에 없는 스트림은 컨슈머가 구독하지 않아 \"메시지 미수신\" 타임아웃이 된다")
                .isEqualTo(streams(PRODUCTION_REDIS).keySet());
    }

    @Test
    void redis_스트림별_스레드풀과_최대길이가_운영과_테스트에서_같다() {
        final Map<String, Object> production = streams(PRODUCTION_REDIS);
        final Map<String, Object> test = streams(TEST_REDIS);

        SoftAssertions.assertSoftly(
                softly -> production.keySet().stream().filter(test::containsKey).forEach(key -> {
                    softly.assertThat(comparableSettings(test.get(key)))
                            .as("스트림 %s 설정이 운영과 다르다", key)
                            .isEqualTo(comparableSettings(production.get(key)));
                }));
    }

    @Test
    void 공유_스레드풀의_core_size가_그_풀을_쓰는_스트림_수_이상이다() {
        SoftAssertions.assertSoftly(softly -> {
            for (final String resource : new String[] {PRODUCTION_REDIS, TEST_REDIS}) {
                final Map<String, Object> pools = asMap(node(resource, "redis", "stream", "thread-pools"));
                final Map<String, Object> streams = streams(resource);
                pools.forEach((poolName, pool) -> {
                    final long subscribers = streams.values().stream()
                            .map(this::asMap)
                            .filter(settings -> poolName.equals(settings.get("thread-pool-name")))
                            .filter(settings -> !Boolean.FALSE.equals(settings.get("listener-enabled")))
                            .count();
                    softly.assertThat(((Number) asMap(pool).get("core-size")).longValue())
                            .as("%s의 %s 풀 — 폴링 태스크는 스레드를 영구 점유한다 (ADR-0022)", resource, poolName)
                            .isGreaterThanOrEqualTo(subscribers);
                });
            }
        });
    }

    @Test
    void 게임_timing_프로퍼티_키셋이_운영과_테스트에서_같다() {
        assertThat(timingKeys(TEST_GAME))
                .as("테스트 설정에 없는 timing 키는 @Validated 바인딩 실패로 게임 컨텍스트 전체를 못 띄운다")
                .isEqualTo(timingKeys(PRODUCTION_GAME));
    }

    /** 리스너를 만들지 않는 스트림(`listener-enabled: false`)은 폴링 스레드를 점유하지 않아 풀 크기 계산에서 빠진다. */
    private Map<String, Object> streams(String resource) {
        return new TreeMap<>(asMap(node(resource, "redis", "stream", "keys")));
    }

    /** 스트림 설정 중 미러링 대상만 남긴다 — 중첩 thread-pool의 queue-capacity는 운영/테스트가 의도적으로 다르다. */
    private Map<String, Object> comparableSettings(Object settings) {
        final Map<String, Object> comparable = new TreeMap<>();
        asMap(settings).forEach((key, value) -> {
            if (INTENTIONALLY_DIFFERENT.contains(key)) {
                return;
            }
            comparable.put(key, value instanceof Map<?, ?> ? comparableSettings(value) : value);
        });
        return comparable;
    }

    private Set<String> timingKeys(String resource) {
        final Set<String> keys = new TreeSet<>();
        asMap(load(resource)).forEach((game, settings) -> {
            final Object timing = asMap(settings).get("timing");
            if (timing != null) {
                asMap(timing).keySet().forEach(key -> keys.add(game + ".timing." + key));
            }
        });
        return keys;
    }

    private Object node(String resource, String... path) {
        Object current = load(resource);
        for (final String segment : path) {
            current = asMap(current).get(segment);
            assertThat(current)
                    .as("%s 에 %s 경로가 없다", resource, String.join(".", path))
                    .isNotNull();
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object node) {
        return node instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();
    }

    private Object load(String resource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("%s 를 테스트 클래스패스에서 찾지 못했다", resource).isNotNull();
            return new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException(resource + " 를 읽을 수 없다", e);
        }
    }
}
