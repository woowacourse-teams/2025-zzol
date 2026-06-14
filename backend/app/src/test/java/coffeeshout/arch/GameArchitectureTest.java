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

    /**
     * [의도된 예외] MiniGamePersistenceService(game 모듈)가 room.infra.persistence 타입을 직접 사용.
     *
     * MiniGameEntity/MiniGameResultEntity의 JPA @ManyToOne이 RoomEntity/PlayerEntity를 참조하므로
     * game → room.infra 의존은 JPA FK 구조상 단기 해소가 어렵다.
     * 이 의존이 규칙 위반처럼 보일 수 있으나 설계상 허용된 예외다.
     *
     * 해소 방향: MiniGameEntity의 FK를 ID 참조(@Column)로 변경하면 room.infra 의존을 제거할 수 있음.
     */
    /**
     * minigame.application의 room.infra 의존 중 JPA FK 구조상 허용된 클래스 목록:
     * - MiniGamePersistenceService: MiniGameEntity의 RoomEntity FK 생성 (PlayerEntity 생성 책임은
     *   PlayerSnapshotRequiredEvent로 :room에 이관됨 — ADR-0025 PlayerEntity 영속 책임 분리)
     * - MiniGameEntityRepository: RoomEntity FK 파라미터
     *
     * 나머지 application 클래스는 room.infra를 참조해서는 안 된다.
     */
    @ArchTest
    static final ArchRule minigame_persistence_외에는_room_infra를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.minigame.application..")
            .and().haveSimpleNameNotContaining("PersistenceService")
            .and().haveSimpleNameNotContaining("EntityRepository")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room.infra..")
            .as("minigame.application 중 JPA 영속성 관련 클래스를 제외하고는 room.infra를 참조할 수 없다");
}
