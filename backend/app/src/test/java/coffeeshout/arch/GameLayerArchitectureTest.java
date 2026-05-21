package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * :game 모듈 6개 게임의 내부 계층 의존 방향 강제 (domain ← application ← infra).
 * GameArchitectureTest는 게임 간 수평 의존을 막고,
 * 이 테스트는 각 게임 내부 계층 역방향을 막는다.
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class GameLayerArchitectureTest {

    // ─────────────────────────────────────────
    // 공통: 6개 게임 도메인은 infra를 참조할 수 없다
    // ─────────────────────────────────────────

    @ArchTest
    static final ArchRule cardgame_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.cardgame.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.cardgame.infra..")
            .as("cardgame.domain은 cardgame.infra를 참조할 수 없다");

    @ArchTest
    static final ArchRule blockstacking_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.blockstacking.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.blockstacking.infra..")
            .as("blockstacking.domain은 blockstacking.infra를 참조할 수 없다");

    @ArchTest
    static final ArchRule laddergame_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.laddergame.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.laddergame.infra..")
            .as("laddergame.domain은 laddergame.infra를 참조할 수 없다");

    @ArchTest
    static final ArchRule racinggame_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.racinggame.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.racinggame.infra..")
            .as("racinggame.domain은 racinggame.infra를 참조할 수 없다");

    @ArchTest
    static final ArchRule speedtouch_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.speedtouch.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.speedtouch.infra..")
            .as("speedtouch.domain은 speedtouch.infra를 참조할 수 없다");

    @ArchTest
    static final ArchRule blindtimer_domain은_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.blindtimer.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.blindtimer.infra..")
            .as("blindtimer.domain은 blindtimer.infra를 참조할 수 없다");

    // ─────────────────────────────────────────
    // 공통: application은 ui를 참조할 수 없다
    // ─────────────────────────────────────────

    @ArchTest
    static final ArchRule cardgame_application은_ui를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.cardgame.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.cardgame.ui..")
            .as("cardgame.application은 cardgame.ui를 참조할 수 없다");

    @ArchTest
    static final ArchRule laddergame_application은_ui를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.laddergame.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.laddergame.ui..")
            .as("laddergame.application은 laddergame.ui를 참조할 수 없다");

    @ArchTest
    static final ArchRule racinggame_application은_ui를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.racinggame.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.racinggame.ui..")
            .as("racinggame.application은 racinggame.ui를 참조할 수 없다");

    @ArchTest
    static final ArchRule minigame_application은_ui를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.minigame.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.minigame.ui..")
            .as("minigame.application은 minigame.ui를 참조할 수 없다");
}
