package coffeeshout.contract;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.app.IntegrationTestSupport;
import coffeeshout.websocket.docs.WsCatalog;
import coffeeshout.websocket.docs.WsCatalogBuilder;
import coffeeshout.websocket.docs.WsReceive;
import coffeeshout.websocket.docs.WsTopic;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Component;

/**
 * FE 가 소비하는 WebSocket 계약의 원천을 검증한다.
 *
 * <p>{@code @WsTopic.path} 는 손으로 적는 리터럴이고 실제 발행은 별개의 format 상수를 쓴다.
 * 둘이 어긋나면 카탈로그가 거짓말을 하고, 그걸 SSOT 로 삼는 FE 검사까지 함께 틀린다.
 * 여기서 양방향으로 대조해 어긋난 순간 빨개지게 한다.
 */
@DisplayName("WebSocket 카탈로그 컨트랙트")
class WsCatalogContractTest extends IntegrationTestSupport {

    private static final Path FIXTURE = Path.of("src", "test", "resources", "__fixtures__", "ws-catalog.json");
    private static final String TOPIC_PREFIX = "/topic/";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WsCatalogBuilder catalogBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("발행 경로 상수와 @WsTopic 선언이 일치한다")
    void 발행_경로_상수와_WsTopic_선언이_일치한다() {
        final Set<String> declared = catalogBuilder.build().topics().stream()
                .map(topic -> normalize(topic.path()))
                .collect(Collectors.toCollection(TreeSet::new));

        final Set<String> published = componentClasses()
                .flatMap(type -> Arrays.stream(type.getDeclaredFields()))
                .filter(WsCatalogContractTest::isStaticString)
                .map(WsCatalogContractTest::readStaticString)
                .filter(value -> value.startsWith(TOPIC_PREFIX))
                .map(WsCatalogContractTest::normalize)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(published)
                .as("발행 상수에만 있으면 @WsTopic 누락, 카탈로그에만 있으면 상수 없이 발행하거나 죽은 선언이다")
                .isEqualTo(declared);
    }

    @Test
    @DisplayName("@MessageMapping 메서드는 @WsReceive 또는 @WsTopic 을 갖는다")
    void MessageMapping_메서드는_수신_문서를_갖는다() {
        final Set<String> undocumented = componentClasses()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods())
                        .filter(method -> AnnotationUtils.findAnnotation(method, MessageMapping.class) != null)
                        .filter(WsCatalogContractTest::lacksReceiveDoc)
                        .map(method -> type.getSimpleName() + "#" + method.getName()))
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(undocumented)
                .as("@WsReceive 나 @WsTopic 이 없으면 send 가 카탈로그에서 빠져 FE 가 그 destination 을 못 쓴다")
                .isEmpty();
    }

    @Test
    @DisplayName("카탈로그를 fixture 로 기록한다")
    void 카탈로그를_fixture_로_기록한다() throws Exception {
        assertThat(FIXTURE.toAbsolutePath().toString())
                .as("Test 태스크의 작업 디렉터리는 :app 이라 이 상대경로가 커밋본을 가리켜야 한다")
                .endsWith("app/src/test/resources/__fixtures__/ws-catalog.json");

        Files.createDirectories(FIXTURE.getParent());
        Files.writeString(FIXTURE, serialize(catalogBuilder.build()));
    }

    /**
     * 줄바꿈을 {@code \n} 으로 못 박는다. {@code SerializationFeature.INDENT_OUTPUT} 의 기본
     * {@code DefaultIndenter} 는 {@code System.lineSeparator()} 를 써서 OS 마다 다른 파일이 나온다.
     */
    private String serialize(WsCatalog catalog) throws Exception {
        final DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        final DefaultPrettyPrinter printer =
                new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter);
        return objectMapper.writer(printer).writeValueAsString(catalog) + "\n";
    }

    private Stream<Class<?>> componentClasses() {
        return applicationContext.getBeansWithAnnotation(Component.class).values().stream()
                .map(AopUtils::getTargetClass)
                .distinct();
    }

    private static boolean lacksReceiveDoc(Method method) {
        return AnnotationUtils.findAnnotation(method, WsReceive.class) == null
                && method.getAnnotationsByType(WsTopic.class).length == 0;
    }

    private static boolean isStaticString(Field field) {
        return Modifier.isStatic(field.getModifiers()) && field.getType() == String.class;
    }

    private static String readStaticString(Field field) {
        try {
            field.setAccessible(true);
            final Object value = field.get(null);
            return value instanceof String text ? text : "";
        } catch (IllegalAccessException e) {
            return "";
        }
    }

    /**
     * 경로 변수 표기를 한 자리표시자로 접는다. {@code %s}(발행 상수), {@code {joinCode}}(애노테이션),
     * {@code {joinCode:.{4}}}(중첩 중괄호) 가 모두 {@code {}} 가 된다. 중첩 중괄호 때문에 정규식으로는
     * 안전하게 못 잡아 세그먼트를 통째로 바꾼다.
     */
    static String normalize(String path) {
        return Arrays.stream(path.split("/", -1))
                .map(segment -> segment.contains("{") || segment.contains("%s") ? "{}" : segment)
                .collect(Collectors.joining("/"));
    }
}
