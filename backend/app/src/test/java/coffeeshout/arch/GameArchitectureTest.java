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

    // :game 모듈의 프로덕션 패키지 루트 — 도메인 모듈(room/user) 참조 금지 규칙이 공유한다.
    private static final String[] GAME_PACKAGES = {
            "coffeeshout.minigame..",
            "coffeeshout.cardgame..",
            "coffeeshout.blockstacking..",
            "coffeeshout.laddergame..",
            "coffeeshout.racinggame..",
            "coffeeshout.speedtouch..",
            "coffeeshout.blindtimer..",
            "coffeeshout.nunchi..",
            "coffeeshout.game.."
    };

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
     * :game 프로덕션 코드는 :room을 직접 참조할 수 없다. ADR-0025가 유예했던 JPA FK 계열 예외를 ADR-0034가
     * 제거했다 — 미니게임 영속 엔티티는 room_session/player를 {@code Long} FK 컬럼으로 참조하고, 그 id·상태전이는
     * {@code :game-api}의 {@code RoomSnapshotQuery} 포트·이벤트로 {@code :room}이 공급/수행한다.
     *
     * <p>(@AnalyzeClasses가 테스트를 제외하므로 main 소스만 검사한다 — 통합 테스트 컨텍스트가 전이 :room 빈을
     * 로드하는 것은 무관하다.) 재유입 시 이 규칙이 실패한다.
     */
    @ArchTest
    static final ArchRule game_프로덕션은_room을_직접_참조할_수_없다 = noClasses()
            .that().resideInAnyPackage(GAME_PACKAGES)
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.room..")
            .as("game 프로덕션 코드는 room을 직접 참조할 수 없다 — 방·플레이어 id·상태전이는 RoomSnapshotQuery 포트·이벤트로 처리한다 (ADR-0034)");

    /**
     * :game 프로덕션 코드는 :user를 직접 참조할 수 없다. (@AnalyzeClasses가 테스트를 제외하므로
     * main 소스만 검사한다 — 통합 테스트 컨텍스트가 전이 :user 빈을 mock 등록하는 것은 무관하다.)
     *
     * 유저 통계 갱신은 :game이 결과 저장 후 발행하는 MiniGameStatsRecordedEvent(:game-api)를
     * :user가 구독해 처리한다 — {@code game → user} 프로덕션 의존을 이벤트로 역전했다(이슈 #1547).
     * 재유입 시 이 규칙이 실패한다.
     */
    @ArchTest
    static final ArchRule game_프로덕션은_user를_직접_참조할_수_없다 = noClasses()
            .that().resideInAnyPackage(GAME_PACKAGES)
            .should().dependOnClassesThat()
            .resideInAPackage("coffeeshout.user..")
            .as("game 프로덕션 코드는 user를 직접 참조할 수 없다 — 유저 통계는 MiniGameStatsRecordedEvent 구독으로 처리한다 (이슈 #1547)");
}
