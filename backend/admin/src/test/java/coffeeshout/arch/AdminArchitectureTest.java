package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * :admin 모듈 내 도메인(dashboard, patchnote, report) 간 직접 참조 금지.
 * 세 영역은 운영자 영역이라는 이유로 한 모듈에 있지만 서로 독립적으로 유지해야 한다.
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
}
