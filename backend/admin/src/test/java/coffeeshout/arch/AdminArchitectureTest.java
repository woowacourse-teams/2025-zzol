package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * :admin 모듈 내 도메인(dashboard, patchnote, report) 간 직접 참조 금지.
 * 세 영역은 운영자 영역이라는 이유로 한 모듈에 있지만 서로 독립적으로 유지해야 한다.
 *
 * <p>백오피스 재설계로 들어온 {@code coffeeshout.admin.*} 하위 영역에는 상호 참조 금지를
 * 걸지 않았다. overview 는 정의상 여러 영역(report, profanity, ipblock)을 모아 보여주는
 * 화면이고 quality 도 신고와 검열을 함께 잰다. 모으는 것이 목적인 영역에 그 규칙을 걸면
 * 우회하려고 의미 없는 중간 계층이 생긴다.
 *
 * <p>"기존 영역이 admin 하위를 참조하면 안 된다" 같은 방향 규칙도 두지 않았다.
 * {@code report}가 이미 {@code admin.ipblock}(신고자 IP 해제)과 {@code admin.support}(공용
 * 페이지 응답)를 정당하게 쓰고 있어 규칙과 코드가 처음부터 어긋난다. 예외를 달아 가며
 * 유지하는 규칙은 곧 아무도 안 읽게 된다.
 *
 * <p>"공용 계층({@code admin.support})은 도메인을 참조하지 않는다"는 한때 걸지 못했다.
 * 그 패키지의 {@code AdminViewExceptionHandler}가 Thymeleaf 컨트롤러를
 * {@code assignableTypes}로 지목해 report 와 ipblock 을 알고 있었기 때문이다.
 * 그 클래스가 Thymeleaf 와 함께 사라져 이제 {@code PageResponse} 하나만 남았고,
 * 도메인 import 가 0건이라 규칙을 걸 수 있게 됐다. 아래 마지막 규칙이 그것이다.
 *
 * <p>즉 아래 일곱 규칙이 전부다. 코드가 지키지 못하는 규칙을 예외를 달아 가며 유지하면
 * 곧 아무도 읽지 않는 장식이 된다.
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class AdminArchitectureTest {

    @ArchTest
    static final ArchRule dashboard는_patchnote를_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.dashboard..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("coffeeshout.patchnote..")
            .as("dashboard는 patchnote를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule dashboard는_report를_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.dashboard..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("coffeeshout.report..")
            .as("dashboard는 report를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule patchnote는_dashboard를_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.patchnote..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("coffeeshout.dashboard..")
            .as("patchnote는 dashboard를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule patchnote는_report를_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.patchnote..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("coffeeshout.report..")
            .as("patchnote는 report를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule report는_dashboard를_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.report..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("coffeeshout.dashboard..")
            .as("report는 dashboard를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule report는_patchnote를_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.report..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("coffeeshout.patchnote..")
            .as("report는 patchnote를 직접 참조할 수 없다");

    /**
     * 공용 계층은 도메인을 모른다.
     *
     * <p>{@code admin.support}는 {@code PageResponse}처럼 어느 도메인에서나 쓰는 것만 담는다.
     * 여기서 특정 도메인을 참조하기 시작하면 그 도메인을 쓰지 않는 화면까지 같이 컴파일되고,
     * 나중에 도메인 하나를 떼어낼 때 공용 계층이 붙잡는다.
     */
    @ArchTest
    static final ArchRule support는_도메인을_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.admin.support..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("coffeeshout.report..", "coffeeshout.patchnote..", "coffeeshout.dashboard..");
}
