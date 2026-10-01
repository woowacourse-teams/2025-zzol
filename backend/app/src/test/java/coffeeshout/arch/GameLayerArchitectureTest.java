package coffeeshout.arch;

import static coffeeshout.arch.GameArchitectureTest.GAMES;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * :game 모듈 각 게임의 내부 계층 의존 방향 강제 (domain ← application ← infra).
 * GameArchitectureTest는 게임 간 수평 의존을 막고,
 * 이 테스트는 각 게임 내부 계층 역방향을 막는다.
 * 검사 대상 게임 목록은 GameArchitectureTest.GAMES 하나로 관리한다.
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class GameLayerArchitectureTest {

    @ArchTest
    static void 게임_domain은_infra를_참조할_수_없다(JavaClasses classes) {
        for (final String game : GAMES) {
            noClasses()
                    .that()
                    .resideInAPackage("coffeeshout." + game + ".domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("coffeeshout." + game + ".infra..")
                    .as(game + ".domain은 " + game + ".infra를 참조할 수 없다")
                    .check(classes);
        }
    }

    @ArchTest
    static void 게임_application은_ui를_참조할_수_없다(JavaClasses classes) {
        for (final String game : GAMES) {
            noClasses()
                    .that()
                    .resideInAPackage("coffeeshout." + game + ".application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("coffeeshout." + game + ".ui..")
                    .as(game + ".application은 " + game + ".ui를 참조할 수 없다")
                    .check(classes);
        }
    }

    @ArchTest
    static final ArchRule minigame_application은_ui를_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.minigame.application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("coffeeshout.minigame.ui..")
            .as("minigame.application은 minigame.ui를 참조할 수 없다");
}
