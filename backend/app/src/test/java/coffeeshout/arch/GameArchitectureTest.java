package coffeeshout.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.minigame.domain.MiniGameType;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * :game 모듈 내 게임 간 직접 참조 금지.
 * 게임은 game-api(:game-api)의 추상화에만 의존해야 하며
 * 다른 게임 도메인을 직접 import하면 안 된다.
 */
@AnalyzeClasses(packages = "coffeeshout", importOptions = ImportOption.DoNotIncludeTests.class)
public class GameArchitectureTest {

    // 개별 게임 패키지명. MiniGameType 상수 하나당 하나 — 개수가 어긋나면 아래 단언이 실패한다.
    static final String[] GAMES = {
        "cardgame", "blockstacking", "laddergame", "racinggame", "speedtouch", "blindtimer", "nunchi", "wormgame"
    };

    private static final String[] GAME_PACKAGE_PATTERNS =
            Arrays.stream(GAMES).map(game -> "coffeeshout." + game + "..").toArray(String[]::new);

    // :game 모듈의 프로덕션 패키지 루트 — 도메인 모듈(room/user) 참조 금지 규칙이 공유한다.
    private static final String[] GAME_PACKAGES = Stream.concat(
                    Arrays.stream(GAME_PACKAGE_PATTERNS), Stream.of("coffeeshout.minigame..", "coffeeshout.game.."))
            .toArray(String[]::new);

    @ArchTest
    static void 게임_목록은_MiniGameType과_개수가_같다(JavaClasses classes) {
        assertThat(GAMES).as("새 게임을 추가했으면 GAMES 에도 패키지명을 넣어야 아키텍처 검사 대상이 된다").hasSameSizeAs(MiniGameType.values());
    }

    @ArchTest
    static void 게임은_다른_게임을_참조할_수_없다(JavaClasses classes) {
        for (final String game : GAMES) {
            final String self = "coffeeshout." + game + "..";
            final String[] others = Arrays.stream(GAME_PACKAGE_PATTERNS)
                    .filter(pkg -> !pkg.equals(self))
                    .toArray(String[]::new);
            noClasses()
                    .that()
                    .resideInAPackage(self)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(others)
                    .as(game + "은 다른 게임 패키지를 직접 참조할 수 없다")
                    .check(classes);
        }
    }

    @ArchTest
    static final ArchRule minigame_orchestration은_개별_게임을_직접_참조할_수_없다 = noClasses()
            .that()
            .resideInAPackage("coffeeshout.minigame..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(GAME_PACKAGE_PATTERNS)
            .as("minigame orchestration은 개별 게임 패키지를 직접 참조할 수 없다 — MiniGameFactory SPI를 통해 디스패치해야 한다");

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
            .that()
            .resideInAnyPackage(GAME_PACKAGES)
            .should()
            .dependOnClassesThat()
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
            .that()
            .resideInAnyPackage(GAME_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("coffeeshout.user..")
            .as("game 프로덕션 코드는 user를 직접 참조할 수 없다 — 유저 통계는 MiniGameStatsRecordedEvent 구독으로 처리한다 (이슈 #1547)");
}
