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
 * <p>서비스 테스트는 DTO를 보므로 직렬화가 원문을 다시 끌어오는 경로를 못 잡는다. 등록 리스너도 실제 이벤트
 * 퍼블리셔와 읽기 전용 트랜잭션 경계를 거쳐야 돌므로, 목 퍼블리셔를 쓰는 서비스 테스트에서는 보이지 않는다.
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
    void 닉네임을_내보내는_엔드포인트는_검열_전_닉네임의_원문을_싣지_않고_검열_큐에_넣는다(String endpoint) throws Exception {
        final String body = mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(body).doesNotContain(ORIGINAL).contains("처리 중인 닉네임").contains("\"masked\":true");
            softly.assertThat(nicknameAuditJpaRepository.findNicknamesByStatus(NicknameAuditStatus.UNAUDITED))
                    .containsOnly(ORIGINAL);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"/dashboard/top-winners", "/dashboard/racing-game-top-players"})
    void 판정이_CLEAN으로_바뀌면_다음_조회부터_원문이_보인다(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint)).andExpect(status().isOk());
        final NicknameAudit audit = nicknameAuditJpaRepository
                .findAllByNicknameIn(List.of(ORIGINAL))
                .getFirst();
        audit.updateStatus(NicknameAuditStatus.CLEAN);
        nicknameAuditJpaRepository.save(audit);

        final String body = mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains(ORIGINAL).contains("\"masked\":false");
    }
}
