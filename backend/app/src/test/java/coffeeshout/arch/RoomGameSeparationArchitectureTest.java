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

    /**
     * room.application의 게임 패키지 참조는 game-api 이벤트를 수신하는 in-process 리스너(room.application.event)에만
     * 허용한다 — {@code MiniGameResultRoomListener}(결정 5, 게임 종료→확률 조정),
     * {@code RoomGameStartListener}(결정 4, GameSession 시작→방 PLAYING 전이).
     */
    @ArchTest
    static final ArchRule room_application은_게임_이벤트_리스너_외에_minigame을_참조할_수_없다 = noClasses()
            .that().resideInAPackage("coffeeshout.room.application..")
            .and().haveSimpleNameNotContaining("MiniGameResultRoomListener")
            .and().haveSimpleNameNotContaining("RoomGameStartListener")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.minigame..")
            .as("room.application의 게임 패키지 참조는 game-api 이벤트 in-process 리스너 2곳만 허용한다 (ADR-0023)");

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

    /**
     * [ADR-0023 재유입 방지] :game 게임 서비스·도메인·플로우 계층은 Room 애그리거트
     * (coffeeshout.room.domain..)를 직접 참조할 수 없다. 게임 식별은 JoinCode, 플레이어 식별은 Gamer를
     * 사용하며, 게임 인스턴스 조회는 GameSession을 경유한다. "joinCode를 얻으려 Room을 왕복 조회"하던
     * 패턴(RoomQueryService.getByJoinCode → room.getJoinCode())의 재도입을 차단한다.
     *
     * <p>현 상태를 동결(freeze)해 신규 위반을 막는다. 아래는 현재 정당한 :game → room.domain 참조다.
     * <ul>
     *   <li>MiniGamePersistenceService : 결과 영속 시 Room/RoomState/Player 접근 (JPA FK 예외 계열 —
     *       ADR-0023 후속 작업에서 MiniGameEntity의 RoomEntity FK·PlayerEntity 영속 책임 분리 예정)</li>
     * </ul>
     *
     * <p>이전에 정당 참조였던 다음 클래스는 ADR-0023 결정 4·6 개정으로 :room 의존이 제거됐다.
     * <ul>
     *   <li>MiniGameEventService       : 게임 시작을 {@code GameStartReadyEvent}(in-process 동기)로 분리 — 명단은 :room이 실어 전달</li>
     *   <li>MiniGameSelectConsumer     : 호스트 검증을 GameSession으로 이관(권위 세션 사전 생성, Option B)</li>
     *   <li>GameSessionInit/CleanupConsumer : 생명주기 이벤트를 :game-api(GameRoomCreated/RemovedEvent)로 이전</li>
     *   <li>PlayerHands                : 공용 {@code GameErrorCode}(:game-api)로 교체</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule game은_room_domain을_직접_참조할_수_없다 = noClasses()
            .that().resideInAnyPackage(
                    "coffeeshout.minigame..",
                    "coffeeshout.cardgame..",
                    "coffeeshout.blockstacking..",
                    "coffeeshout.laddergame..",
                    "coffeeshout.racinggame..",
                    "coffeeshout.speedtouch..",
                    "coffeeshout.blindtimer.."
            )
            .and().haveSimpleNameNotContaining("MiniGamePersistenceService")
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room.domain..")
            .as("게임 모듈은 Room 애그리거트(coffeeshout.room.domain..)를 직접 참조할 수 없다 "
                    + "— 식별은 JoinCode/Gamer, 게임 조회는 GameSession 경유 (ADR-0023)");
}
