package coffeeshout.dashboard.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.AdminModuleIntegrationTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditJpaRepository;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.infra.persistence.RouletteResultEntity;
import coffeeshout.room.infra.persistence.RouletteResultJpaRepository;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 검열 전 닉네임의 원문이 응답 본문에 실리지 않는지 HTTP 경계에서 본다(#1906).
 *
 * <p>서비스 테스트는 DTO를 보므로 직렬화가 원문을 다시 끌어오는 경로를 못 잡는다. 조회가 검열 테이블에 쓰지
 * 않는다는 것도 실제 이벤트 퍼블리셔가 도는 여기서만 보인다.
 */
class DashboardControllerTest extends AdminModuleIntegrationTest {

    private static final String ORIGINAL = "검열전닉";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private RouletteResultJpaRepository rouletteResultJpaRepository;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Autowired
    private MiniGameResultJpaRepository miniGameResultJpaRepository;

    @Autowired
    private NicknameAuditJpaRepository nicknameAuditJpaRepository;

    /** 검열 행이 없는 회원 하나가 당첨 기록과 미니게임 다섯 개의 기록을 모두 갖는다. */
    @BeforeEach
    void 검열_전_닉네임이_모든_랭킹에_오른다() {
        final RoomEntity room = roomJpaRepository.save(new RoomEntity("MASK"));
        final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", ORIGINAL));
        final PlayerEntity player =
                playerJpaRepository.save(new PlayerEntity(room, ORIGINAL, PlayerType.HOST, user.getId()));
        rouletteResultJpaRepository.save(new RouletteResultEntity(room, player, 10));

        for (final MiniGameType type : List.of(
                MiniGameType.RACING_GAME,
                MiniGameType.BLOCK_STACKING,
                MiniGameType.WORM_GAME,
                MiniGameType.SPEED_TOUCH,
                MiniGameType.BLIND_TIMER)) {
            final MiniGameEntity miniGame = miniGameJpaRepository.save(new MiniGameEntity(room.getId(), type));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, player.getId(), 1, 500L));
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/dashboard/top-winners",
                "/dashboard/lowest-probability-winner",
                "/dashboard/racing-game-top-players",
                "/dashboard/block-stacking-top-players",
                "/dashboard/worm-game-top-players",
                "/dashboard/speed-touch-top-players",
                "/dashboard/blind-timer-top-players"
            })
    void 닉네임을_내보내는_엔드포인트는_검열_전_닉네임의_원문과_유저_코드를_싣지_않는다(String endpoint) throws Exception {
        final String body = 조회(endpoint);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(body).doesNotContain(ORIGINAL, "AB3CD");
            // 등록은 닉네임을 저장할 때 한다. 공개 GET은 검열 테이블에 쓰지 않는다.
            softly.assertThat(nicknameAuditJpaRepository.findAllByNicknameIn(List.of(ORIGINAL)))
                    .isEmpty();
        });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/dashboard/racing-game-top-players",
                "/dashboard/block-stacking-top-players",
                "/dashboard/worm-game-top-players",
                "/dashboard/speed-touch-top-players",
                "/dashboard/blind-timer-top-players"
            })
    void 미니게임_랭킹은_판정_전_이름_자리에_고정_문구를_싣는다(String endpoint) throws Exception {
        final String body = 조회(endpoint);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(body).contains("처리 중인 닉네임").contains("\"masked\":true");
            // 이름 VO는 펼쳐서 내보낸다. 감싼 객체로 나가면 기존 프론트가 이름을 못 읽는다.
            softly.assertThat(body).doesNotContain("\"player\":");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"/dashboard/top-winners", "/dashboard/lowest-probability-winner"})
    void 당첨자_슬라이드는_판정_전_회원을_고정_문구로도_싣지_않는다(String endpoint) throws Exception {
        assertThat(조회(endpoint)).doesNotContain("처리 중인 닉네임");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/dashboard/top-winners", "/dashboard/racing-game-top-players"})
    void 판정이_CLEAN으로_바뀌면_다음_조회부터_원문이_보인다(String endpoint) throws Exception {
        final NicknameAudit audit = new NicknameAudit(ORIGINAL);
        audit.updateStatus(NicknameAuditStatus.CLEAN);
        nicknameAuditJpaRepository.save(audit);

        assertThat(조회(endpoint)).contains(ORIGINAL);
    }

    private String 조회(String endpoint) throws Exception {
        return mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
