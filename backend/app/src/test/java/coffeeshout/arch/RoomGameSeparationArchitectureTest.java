package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ADR-0023: Room과 GameSession 소유권 분리 경계 강제.
 *
 * - room.domain은 게임 타입(MiniGameType, MiniGameResult, Playable 등)을 참조할 수 없다.
 *   gamecommon의 JoinCode·Gamer 식별자 참조는 허용한다.
 * - room.application의 게임 패키지 참조는 MiniGameFinishedEvent 리스너 한 곳만 허용한다.
 * - :game은 room.domain.player를 참조할 수 없다 — 플레이어 식별은 Gamer를 사용한다.
 *   (Player → Gamer 변환은 :room의 Room.getGamers()에서 완료된다)
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class RoomGameSeparationArchitectureTest {

    @ArchTest
    static final ArchRule room_domain은_minigame_패키지를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.minigame..")
            .as("room.domain은 게임 타입(coffeeshout.minigame..)을 참조할 수 없다 — 게임 소유권은 GameSession에 있다 (ADR-0023)");

    /**
     * gamecommon 중 JoinCode·Gamer 식별자와 MiniGameResultType(룰렛 확률 조정이 소비하는
     * 결과 의미 타입)은 room.domain 참조가 허용된다. 게임 SPI 두 종만 금지한다.
     */
    @ArchTest
    static final ArchRule room_domain은_게임_SPI를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.domain..")
            .should().dependOnClassesThat()
            .haveNameMatching("coffeeshout\\.gamecommon\\.(Playable|MiniGameFactory)")
            .as("room.domain은 게임 SPI(Playable, MiniGameFactory)를 참조할 수 없다 (ADR-0023)");

    @ArchTest
    static final ArchRule room_application은_결과_리스너_외에_minigame을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.application..")
            .and().haveSimpleNameNotContaining("MiniGameResultRoomListener")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.minigame..")
            .as("room.application의 게임 패키지 참조는 MiniGameResultRoomListener 한 곳만 허용한다 (ADR-0023)");

    /**
     * [의도된 예외] MiniGamePersistenceService는 PlayerEntity 영속 필드(name, playerType, userId)를
     * 채우기 위해 room.getPlayers()의 Player에 접근한다. Gamer는 playerType을 갖지 않아 대체 불가.
     * GameArchitectureTest의 room.infra JPA FK 예외와 같은 계열이며, 해소 방향도 동일하다
     * (FK를 ID 참조로 변경 시 함께 제거 가능).
     */
    @ArchTest
    static final ArchRule game은_room_domain_player를_참조할_수_없다 = noClasses()
            .that().resideInAnyPackage(
                    "coffeeshout.minigame..",
                    "coffeeshout.cardgame..",
                    "coffeeshout.blockstacking..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.speedtouch..",
                    "coffeeshout.blindtimer.."
            )
            .and().haveSimpleNameNotContaining("PersistenceService")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room.domain.player..")
            .as("게임 모듈은 room.domain.player를 참조할 수 없다 — 플레이어 식별은 Gamer(:game-api)를 사용한다 (ADR-0023)");
}
