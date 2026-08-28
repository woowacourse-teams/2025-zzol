package coffeeshout.arch;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.config.GameSchedulerTestConfig;
import coffeeshout.config.IntegrationSchedulerTestConfig;
import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.support.CommonTestSchedulerConfig;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.TaskScheduler;

/**
 * 전용 스케줄러 빈을 프로덕션에만 추가하고 테스트 미러를 빠뜨리는 사고를 막는다
 * (postmortem 0004 / PR #1484에서 IT 55건 실패).
 *
 * <p>Spring 컨텍스트를 띄우지 않는 순수 리플렉션 검사라 Docker도 컨텍스트 로딩도 필요 없다 —
 * 미러가 빠졌다는 사실을 IT 무더기 실패가 아니라 이 테스트 하나로 알게 하는 것이 목적이다.
 */
class SchedulerMirrorTest {

    private static final String PRODUCTION_ONLY_PROFILE = "!test";

    private static final List<Class<?>> SCHEDULER_TYPES = List.of(TaskScheduler.class, FlowScheduler.class);

    /**
     * ponytail: 이름으로 성격을 판단한다. {@code *ThreadPoolTaskScheduler}는 같은 config 안의
     * {@code @Profile("!test")} FlowScheduler에만 주입되는 내부 배선이라, 테스트가 그 FlowScheduler를
     * 통째로 갈아끼우면 소비자가 사라져 미러가 필요 없다. 다른 이름의 스케줄러가 생기면 미러를 요구한다.
     * 주입 관계를 실제로 따라가야 할 만큼 배선이 복잡해지면 그때 판정을 바꾼다.
     */
    private static final String INTERNAL_WIRING_SUFFIX = "ThreadPoolTaskScheduler";

    @Test
    void 프로덕션_전용_스케줄러_빈은_모두_같은_이름의_테스트_미러를_갖는다() {
        final Set<String> production = productionSchedulerBeanNames();
        final Set<String> mirrored = beanNames(GameSchedulerTestConfig.class, CommonTestSchedulerConfig.class);

        assertThat(production)
                .as("@Profile(\"!test\") 스케줄러 빈이 하나도 안 잡혔다면 스캔 대상이 잘못된 것이다")
                .isNotEmpty();
        assertThat(mirrored)
                .as("프로덕션 스케줄러 빈은 GameSchedulerTestConfig 또는 CommonTestSchedulerConfig에 "
                        + "같은 이름으로 있어야 한다 (없으면 그 빈을 쓰는 모듈의 테스트가 NoSuchBeanDefinitionException으로 깨진다)")
                .containsAll(production);
    }

    @Test
    void 서비스테스트_미러의_게임_스케줄러는_통합테스트_미러에도_같은_이름으로_있다() {
        final Set<String> serviceTestMirror = beanNames(GameSchedulerTestConfig.class);
        final Set<String> integrationMirror = beanNames(IntegrationSchedulerTestConfig.class);

        assertThat(integrationMirror)
                .as("두 미러가 어긋나면 한쪽을 쓰는 테스트만 깨져 로컬에서 \"내 모듈은 통과\"로 보인다")
                .containsAll(serviceTestMirror);
    }

    private Set<String> productionSchedulerBeanNames() {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));

        final Set<String> names = new TreeSet<>();
        for (final BeanDefinition definition : scanner.findCandidateComponents("coffeeshout")) {
            final Class<?> configClass = load(definition.getBeanClassName());
            if (configClass.isAnnotationPresent(TestConfiguration.class)) {
                continue;
            }
            for (final Method method : configClass.getDeclaredMethods()) {
                if (isProductionOnlyScheduler(method) && !beanName(method).endsWith(INTERNAL_WIRING_SUFFIX)) {
                    names.add(beanName(method));
                }
            }
        }
        return names;
    }

    private boolean isProductionOnlyScheduler(Method method) {
        final Profile profile = method.getAnnotation(Profile.class);
        return method.isAnnotationPresent(Bean.class)
                && profile != null
                && Arrays.asList(profile.value()).contains(PRODUCTION_ONLY_PROFILE)
                && SCHEDULER_TYPES.stream().anyMatch(type -> type.isAssignableFrom(method.getReturnType()));
    }

    private Set<String> beanNames(Class<?>... configClasses) {
        final Set<String> names = new TreeSet<>();
        for (final Class<?> configClass : configClasses) {
            for (final Method method : configClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class)) {
                    names.add(beanName(method));
                }
            }
        }
        return names;
    }

    private String beanName(Method method) {
        final Bean bean = method.getAnnotation(Bean.class);
        final String[] declared = bean.name().length > 0 ? bean.name() : bean.value();
        return declared.length > 0 ? declared[0] : method.getName();
    }

    private Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("스캔된 설정 클래스를 로드할 수 없다: " + className, e);
        }
    }
}
