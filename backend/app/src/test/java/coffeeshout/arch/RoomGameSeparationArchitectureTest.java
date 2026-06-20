package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ADR-0025: Room과 GameSession 소유권 분리 경계 강제.
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
            .as("room.domain은 게임 타입(coffeeshout.minigame..)을 참조할 수 없다 — 게임 소유권은 GameSession에 있다 (ADR-0025)");

    /**
     * gamecommon 중 JoinCode·Gamer 식별자와 MiniGameResultType(룰렛 확률 조정이 소비하는
     * 결과 의미 타입)은 room.domain 참조가 허용된다. 게임 SPI 두 종만 금지한다.
     */
    @ArchTest
    static final ArchRule room_domain은_게임_SPI를_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.domain..")
            .should().dependOnClassesThat()
            .haveNameMatching("coffeeshout\\.gamecommon\\.(Playable|MiniGameFactory)")
            .as("room.domain은 게임 SPI(Playable, MiniGameFactory)를 참조할 수 없다 (ADR-0025)");

    /**
     * room.application의 게임 패키지 참조는 game-api 이벤트를 수신하는 in-process 리스너(room.application.event)에만
     * 허용한다 — {@code MiniGameResultRoomListener}(결정 5, 게임 종료→확률 조정),
     * {@code RoomGameStartListener}(결정 4, GameSession 시작→방 PLAYING 전이),
     * {@code PlayerSnapshotListener}(PlayerEntity 영속 책임 분리, 첫 게임 시작→플레이어 스냅샷 저장).
     */
    @ArchTest
    static final ArchRule room_application은_게임_이벤트_리스너_외에_minigame을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.application..")
            .and().haveSimpleNameNotContaining("MiniGameResultRoomListener")
            .and().haveSimpleNameNotContaining("RoomGameStartListener")
            .and().haveSimpleNameNotContaining("PlayerSnapshotListener")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.minigame..")
            .as("room.application의 게임 패키지 참조는 game-api 이벤트 in-process 리스너 3곳만 허용한다 (ADR-0025)");

    /**
     * :game은 room.domain.player를 참조할 수 없다 — 플레이어 식별은 Gamer(:game-api)를 사용한다.
     * (ADR-0025 FK 영속 책임 분리 완료로, PlayerEntity 영속을 위해 Player에 접근하던
     * MiniGamePersistenceService 예외가 제거됐다 — 이제 RoomReferencePort.findPlayerRefs로 ID·userId만 받는다.)
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
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room.domain.player..")
            .as("게임 모듈은 room.domain.player를 참조할 수 없다 — 플레이어 식별은 Gamer(:game-api)를 사용한다 (ADR-0025)");

    /**
     * [ADR-0025 재유입 방지] :game(게임 서비스·도메인·플로우·영속·UI)은 :room(coffeeshout.room..) 전체를
     * 직접 참조할 수 없다. 게임 식별은 JoinCode, 플레이어 식별은 Gamer를 쓰고, 게임 인스턴스 조회는
     * GameSession을 경유하며, :room의 식별·상태·영속이 필요하면 RoomReferencePort(:game-api)를 통한다.
     * "joinCode를 얻으려 Room을 왕복 조회"하던 패턴(RoomQueryService.getByJoinCode)의 재도입도 차단한다.
     *
     * <p>FK 영속 책임 분리 완료로, 이전의 정당 참조(MiniGamePersistenceService·MiniGameResultSaveEventListener의
     * RoomEntity/PlayerEntity/RoomState/RoomQueryService 직접 참조)가 모두 제거돼 더 이상 예외 클래스가 없다.
     * room.domain뿐 아니라 room.application·room.infra까지 포함한 :room 전체로 금지 범위를 격상한다.
     */
    @ArchTest
    static final ArchRule game은_room을_직접_참조할_수_없다 = noClasses()
            .that().resideInAnyPackage(
                    "coffeeshout.minigame..",
                    "coffeeshout.cardgame..",
                    "coffeeshout.blockstacking..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.speedtouch..",
                    "coffeeshout.blindtimer.."
            )
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room..")
            .as("게임 모듈은 :room(coffeeshout.room..)을 직접 참조할 수 없다 "
                    + "— 식별은 JoinCode/Gamer, 게임 조회는 GameSession, room 참조는 RoomReferencePort(:game-api) 경유 (ADR-0025)");
}
