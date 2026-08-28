package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import coffeeshout.global.exception.ErrorCode;
import coffeeshout.global.exception.custom.CoffeeShoutException;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

/**
 * 예외 계층·클래스 네이밍 컨벤션 강제 — 리뷰 코멘트로만 잡히던 규칙을 CI로 옮긴다
 * (규칙 텍스트의 SSOT는 {@code docs/conventions-production.md}).
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class ConventionArchitectureTest {

    /**
     * 최상위 예외 클래스만 본다. 중첩 예외는 선언한 클래스 밖으로 나가지 않는 제어 신호로만 쓰이고
     * (예: {@code SettlementMessageProcessor.PoisonMessageException} — 두 프레임 위에서 타입으로 잡힌다),
     * 응답·로그에 실려 나가지 않으므로 식별 코드를 붙일 이유가 없다. 파일 하나를 차지하는 예외는
     * 언젠가 밖으로 던져질 것으로 본다.
     */
    @ArchTest
    static final ArchRule 예외는_CoffeeShoutException_계열이다 = classes()
            .that()
            .areAssignableTo(RuntimeException.class)
            .and()
            .areTopLevelClasses()
            .and()
            .resideInAPackage("coffeeshout..")
            .and()
            .doNotBelongToAnyOf(CoffeeShoutException.class)
            .should()
            .beAssignableTo(CoffeeShoutException.class)
            .as("예외는 CoffeeShoutException 계열이어야 한다 — 아니면 ErrorCode 없이 "
                    + "RestExceptionHandler의 Exception 핸들러로 떨어져 응답·로그 어디에도 식별 코드가 남지 않는다");

    /**
     * 익명 클래스는 제외한다 — 제약할 이름 자체가 없다
     * ({@code RestExceptionHandler.toErrorCode}가 응답 조립용으로 하나 만든다).
     */
    @ArchTest
    static final ArchRule ErrorCode_구현체는_ErrorCode로_끝난다 = classes()
            .that()
            .implement(ErrorCode.class)
            .and()
            .areNotAnonymousClasses()
            .should()
            .haveSimpleNameEndingWith("ErrorCode")
            .as("ErrorCode 구현체 이름은 {Domain}ErrorCode 여야 한다");

    /**
     * {@code ..application..} 밖의 {@code @Service}(도메인 서비스·인프라 어댑터)는 대상이 아니다 —
     * 거기서는 {@code *CommandService}·{@code *Generator}처럼 다른 역할 접미사를 쓴다.
     */
    @ArchTest
    static final ArchRule 애플리케이션_계층_서비스는_역할_접미사를_쓴다 = classes()
            .that()
            .areAnnotatedWith(Service.class)
            .and()
            .resideInAPackage("..application..")
            .should()
            .haveSimpleNameEndingWith("Service")
            .orShould()
            .haveSimpleNameEndingWith("FlowOrchestrator")
            .orShould()
            .haveSimpleNameEndingWith("Notifier")
            .as("애플리케이션 계층 @Service 이름은 {Domain}Service·{Domain}FlowOrchestrator·{Domain}Notifier 중 하나여야 한다");
}
