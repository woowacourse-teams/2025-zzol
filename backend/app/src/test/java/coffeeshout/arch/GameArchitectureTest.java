package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * :game 모듈 내 게임 6종 간 직접 참조 금지.
 * 게임은 game-api(:game-api)의 추상화에만 의존해야 하며
 * 다른 게임 도메인을 직접 import하면 안 된다.
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class GameArchitectureTest {

    @ArchTest
    static final ArchRule cardgame은_다른_게임을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.cardgame..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "coffeeshout.blockstacking..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.speedtouch..",
                    "coffeeshout.blindtimer.."
            ).as("cardgame은 다른 게임 패키지를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule blockstacking은_다른_게임을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.blockstacking..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "coffeeshout.cardgame..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.speedtouch..",
                    "coffeeshout.blindtimer.."
            ).as("blockstacking은 다른 게임 패키지를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule laddergame은_다른_게임을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.laddergame..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "coffeeshout.cardgame..",
                    "coffeeshout.blockstacking..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.speedtouch..",
                    "coffeeshout.blindtimer.."
            ).as("laddergame은 다른 게임 패키지를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule racinggame은_다른_게임을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.racinggame..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "coffeeshout.cardgame..",
                    "coffeeshout.blockstacking..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.speedtouch..",
                    "coffeeshout.blindtimer.."
            ).as("racinggame은 다른 게임 패키지를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule speedtouch는_다른_게임을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.speedtouch..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "coffeeshout.cardgame..",
                    "coffeeshout.blockstacking..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.blindtimer.."
            ).as("speedtouch는 다른 게임 패키지를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule blindtimer는_다른_게임을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.blindtimer..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "coffeeshout.cardgame..",
                    "coffeeshout.blockstacking..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.speedtouch.."
            ).as("blindtimer는 다른 게임 패키지를 직접 참조할 수 없다");

    @ArchTest
    static final ArchRule minigame_orchestration은_개별_게임을_직접_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.minigame..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "coffeeshout.cardgame..",
                    "coffeeshout.blockstacking..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.speedtouch..",
                    "coffeeshout.blindtimer.."
            ).as("minigame orchestration은 개별 게임 패키지를 직접 참조할 수 없다 — MiniGameFactory SPI를 통해 디스패치해야 한다");

    // [제거됨] minigame_persistence_외에는_room_infra를_참조할_수_없다
    // ADR-0025 FK 영속 책임 분리 완료(MiniGameEntity/MiniGameResultEntity FK → ID 참조)로 :game → room.infra
    // 의존이 사라졌다. game → :room 전체 금지는 RoomGameSeparationArchitectureTest.game은_room을_직접_참조할_수_없다가
    // room.domain·room.application·room.infra를 모두 포괄해 강제하므로, 부분 집합이던 이 규칙은 제거한다.
}
