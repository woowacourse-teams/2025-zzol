package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.handler.annotation.MessageMapping;

/**
 * WebSocket 핸들러({@code @MessageMapping})는 Redis Stream 비동기 발행의 future를 반환해야 한다.
 *
 * <p>비동기 발행의 실패는 핸들러가 돌아간 뒤에 난다. 핸들러가 future를 반환해야 Spring이 그 실패를
 * {@code @MessageExceptionHandler}로 넘겨 보낸 사용자에게 에러를 보낸다. 반환하지 않고 {@code void}로 두면
 * 그 엔드포인트만 실패 알림이 조용히 사라진다.</p>
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class WebSocketHandlerArchitectureTest {

    @ArchTest
    static final ArchRule MessageMapping_핸들러는_발행_future를_반환한다 = methods()
            .that().areAnnotatedWith(MessageMapping.class)
            .should().haveRawReturnType(CompletableFuture.class)
            .as("@MessageMapping 핸들러는 Redis Stream 발행의 CompletableFuture를 반환해야 한다");
}
