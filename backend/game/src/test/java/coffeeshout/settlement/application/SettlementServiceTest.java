package coffeeshout.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.settlement.application.SettlementService.SettledScore;
import coffeeshout.settlement.domain.SeasonTier;
import coffeeshout.settlement.event.SettlementResultEvent;
import coffeeshout.settlement.event.SettlementResultEvent.PlayerResult;
import coffeeshout.settlement.infra.persistence.SeasonScoreEntity;
import coffeeshout.settlement.infra.persistence.SeasonScoreJpaRepository;
import coffeeshout.settlement.infra.persistence.SeasonSettlementEntity;
import coffeeshout.settlement.infra.persistence.SeasonSettlementJpaRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final long ROOM_SESSION_ID = 7L;
    private static final String GAME_TYPE = "BLIND_TIMER";
    private static final String SEASON = "2026-07";
    private static final Instant EVENT_TIME = Instant.parse("2026-07-15T03:00:00Z");

    @InjectMocks
    SettlementService settlementService;

    @Mock
    SeasonSettlementJpaRepository settlementRepository;
    @Mock
    SeasonScoreJpaRepository scoreRepository;

    @Nested
    class 결과를_정산할_때 {

        @Test
        void 첫_정산이면_원장을_기록하고_시즌_성적을_새로_만든다() {
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 1L))
                    .willReturn(false);
            given(scoreRepository.addPoints(SEASON, 1L, 100)).willReturn(0);
            시즌_성적_조회_설정(1L, 100, SeasonTier.BRONZE);

            List<SettledScore> settled = settlementService.settle(정산_이벤트(new PlayerResult(1L, "한스", 1, 12L)));

            ArgumentCaptor<SeasonSettlementEntity> ledger = ArgumentCaptor.forClass(SeasonSettlementEntity.class);
            verify(settlementRepository).save(ledger.capture());
            assertThat(ledger.getValue().getPoints()).isEqualTo(100);
            assertThat(ledger.getValue().getSeasonKey()).isEqualTo(SEASON);
            verify(scoreRepository).save(any(SeasonScoreEntity.class));
            assertThat(settled).containsExactly(new SettledScore(1L, SEASON, 100, SeasonTier.BRONZE));
        }

        @Test
        void 기존_성적이_있으면_원자_가산만_하고_새_행을_만들지_않는다() {
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 1L))
                    .willReturn(false);
            given(scoreRepository.addPoints(SEASON, 1L, 70)).willReturn(1);
            시즌_성적_조회_설정(1L, 170, SeasonTier.BRONZE);

            settlementService.settle(정산_이벤트(new PlayerResult(1L, "한스", 2, 40L)));

            verify(scoreRepository, never()).save(any(SeasonScoreEntity.class));
        }

        @Test
        void 가산_결과가_임계를_넘으면_티어를_승급한다() {
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 1L))
                    .willReturn(false);
            given(scoreRepository.addPoints(SEASON, 1L, 100)).willReturn(1);
            SeasonScoreEntity score = 시즌_성적_조회_설정(1L, 320, SeasonTier.BRONZE);

            List<SettledScore> settled = settlementService.settle(정산_이벤트(new PlayerResult(1L, "한스", 1, 12L)));

            verify(score).updateTier(SeasonTier.SILVER);
            assertThat(settled.getFirst().tier()).isEqualTo(SeasonTier.SILVER);
        }

        @Test
        void 동점_구간_포인트를_균등_분배해_저장한다() {
            // 2인 동점 1등 → (100+70)/2 = 85. 정책이 아니라 저장 경로가 allRanks를 넘기는지 고정한다
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 1L))
                    .willReturn(false);
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 2L))
                    .willReturn(false);
            given(scoreRepository.addPoints(SEASON, 1L, 85)).willReturn(1);
            given(scoreRepository.addPoints(SEASON, 2L, 85)).willReturn(1);
            시즌_성적_조회_설정(1L, 85, SeasonTier.BRONZE);
            시즌_성적_조회_설정(2L, 85, SeasonTier.BRONZE);

            settlementService.settle(
                    정산_이벤트(new PlayerResult(1L, "한스", 1, 12L), new PlayerResult(2L, "루키", 1, 12L)));

            verify(scoreRepository).addPoints(SEASON, 1L, 85);
            verify(scoreRepository).addPoints(SEASON, 2L, 85);
        }

        @Test
        void 시즌_키는_처리_시각이_아니라_이벤트_시각에서_파생한다() {
            // 재전달된 메시지를 시즌 경계 이후에 재처리해도 같은 시즌으로 정산되어야 멱등이 성립한다
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(anyLong(), anyString(), anyLong()))
                    .willReturn(false);
            given(scoreRepository.addPoints(anyString(), anyLong(), anyInt())).willReturn(1);
            시즌_성적_조회_설정(1L, 100, SeasonTier.BRONZE);

            settlementService.settle(정산_이벤트(new PlayerResult(1L, "한스", 1, 12L)));

            verify(scoreRepository).addPoints(eq(SEASON), eq(1L), anyInt());
        }
    }

    @Nested
    class 재전달된_이벤트를_정산할_때 {

        @Test
        void 이미_정산된_결과는_건너뛴다() {
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 1L))
                    .willReturn(true);

            List<SettledScore> settled = settlementService.settle(정산_이벤트(new PlayerResult(1L, "한스", 1, 12L)));

            verify(settlementRepository, never()).save(any());
            verify(scoreRepository, never()).addPoints(anyString(), anyLong(), anyInt());
            assertThat(settled).isEmpty();
        }

        @Test
        void 동시_경합의_제약_위반은_삼키지_않고_전파한다() {
            // 문서화된 계약: 예외를 여기서 잡으면 재전달 수렴이 끊겨 이중 지급 방어가 무력화된다
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 1L))
                    .willReturn(false);
            given(settlementRepository.save(any(SeasonSettlementEntity.class)))
                    .willThrow(new DataIntegrityViolationException("uk_season_settlement_result"));

            assertThatThrownBy(() -> settlementService.settle(정산_이벤트(new PlayerResult(1L, "한스", 1, 12L))))
                    .isInstanceOf(DataIntegrityViolationException.class);
            verify(scoreRepository, never()).addPoints(anyString(), anyLong(), anyInt());
        }

        @Test
        void 일부만_정산된_이벤트는_남은_결과만_이어서_정산한다() {
            // 크래시로 1L만 정산되고 ACK 전에 재전달된 상황 — 2L만 처리되어야 한다
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 1L))
                    .willReturn(true);
            given(settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, GAME_TYPE, 2L))
                    .willReturn(false);
            given(scoreRepository.addPoints(SEASON, 2L, 70)).willReturn(1);
            시즌_성적_조회_설정(2L, 70, SeasonTier.BRONZE);

            List<SettledScore> settled = settlementService.settle(
                    정산_이벤트(new PlayerResult(1L, "한스", 1, 12L), new PlayerResult(2L, "루키", 2, 40L)));

            assertThat(settled).containsExactly(new SettledScore(2L, SEASON, 70, SeasonTier.BRONZE));
        }
    }

    private SeasonScoreEntity 시즌_성적_조회_설정(long userId, long totalPoints, SeasonTier tier) {
        SeasonScoreEntity score = mock(SeasonScoreEntity.class);
        given(score.getTotalPoints()).willReturn(totalPoints);
        given(score.getTier()).willReturn(tier);
        given(scoreRepository.findBySeasonKeyAndUserId(SEASON, userId)).willReturn(Optional.of(score));
        return score;
    }

    private SettlementResultEvent 정산_이벤트(PlayerResult... results) {
        return new SettlementResultEvent(
                "settlement:" + ROOM_SESSION_ID + ":" + GAME_TYPE,
                EVENT_TIME,
                "AB3C",
                ROOM_SESSION_ID,
                GAME_TYPE,
                List.of(results),
                Arrays.stream(results).map(PlayerResult::rank).toList()
        );
    }
}
