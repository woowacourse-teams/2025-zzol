package coffeeshout.room.application.service.nickname;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.domain.service.PlayerNameGenerator;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerNameRankingCleanupServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-03-21T12:00:00Z"), ZoneOffset.UTC
    );

    @Mock DashboardStatisticsRepository dashboardRepository;
    @Mock
    PlayerNameAuditJpaRepository auditRepository;
    @Mock PlayerJpaRepository playerRepository;
    @Mock
    PlayerNameGenerator playerNameGenerator;

    PlayerNameRankingCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new PlayerNameRankingCleanupService(
                FIXED_CLOCK, dashboardRepository, auditRepository, playerRepository, playerNameGenerator
        );
    }

    @Nested
    class BLOCKED_닉네임이_없는_경우 {

        @Test
        void 랭킹_조회_없이_종료한다() {
            given(auditRepository.findPlayerNamesByStatus(PlayerNameAuditStatus.BLOCKED))
                    .willReturn(Set.of());

            cleanupService.cleanupBlockedNicknames();

            then(dashboardRepository).shouldHaveNoInteractions();
            then(playerRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    class BLOCKED_닉네임이_랭킹에_없는_경우 {

        @Test
        void 교체_없이_종료한다() {
            given(auditRepository.findPlayerNamesByStatus(PlayerNameAuditStatus.BLOCKED))
                    .willReturn(Set.of("씨발"));
            given(dashboardRepository.findTopWinnersBetween(any(), any(), anyInt()))
                    .willReturn(List.of(new TopWinnerResponse("용감한호랑이", 5L)));
            given(dashboardRepository.findRacingGameTopPlayers(any(), any(), anyInt()))
                    .willReturn(List.of());

            cleanupService.cleanupBlockedNicknames();

            then(playerRepository).should(never()).findAllByPlayerName(anyString());
        }
    }

    @Nested
    class BLOCKED_닉네임이_랭킹에_있는_경우 {

        @Test
        void 룰렛_랭킹의_BLOCKED_닉네임을_교체한다() {
            RoomEntity room = mock(RoomEntity.class);
            PlayerEntity player = mock(PlayerEntity.class);
            PlayerEntity roommate = mock(PlayerEntity.class);

            given(auditRepository.findPlayerNamesByStatus(PlayerNameAuditStatus.BLOCKED))
                    .willReturn(Set.of("씨발"));
            given(dashboardRepository.findTopWinnersBetween(any(), any(), anyInt()))
                    .willReturn(List.of(new TopWinnerResponse("씨발", 3L)));
            given(dashboardRepository.findRacingGameTopPlayers(any(), any(), anyInt()))
                    .willReturn(List.of());
            given(playerRepository.findAllByPlayerName("씨발"))
                    .willReturn(List.of(player));
            given(player.getRoomSession()).willReturn(room);
            given(playerRepository.findAllByRoomSession(room))
                    .willReturn(List.of(player, roommate));
            given(player.getPlayerName()).willReturn("씨발");
            given(roommate.getPlayerName()).willReturn("용감한호랑이");
            given(playerNameGenerator.generate(Set.of("씨발", "용감한호랑이")))
                    .willReturn("빠른여우");

            cleanupService.cleanupBlockedNicknames();

            then(player).should().updatePlayerName("빠른여우");
        }

        @Test
        void 레이싱_랭킹의_BLOCKED_닉네임을_교체한다() {
            RoomEntity room = mock(RoomEntity.class);
            PlayerEntity player = mock(PlayerEntity.class);

            given(auditRepository.findPlayerNamesByStatus(PlayerNameAuditStatus.BLOCKED))
                    .willReturn(Set.of("씨발"));
            given(dashboardRepository.findTopWinnersBetween(any(), any(), anyInt()))
                    .willReturn(List.of());
            given(dashboardRepository.findRacingGameTopPlayers(any(), any(), anyInt()))
                    .willReturn(List.of(new RacingGameTopPlayerResponse("씨발", 1.5, 100L)));
            given(playerRepository.findAllByPlayerName("씨발"))
                    .willReturn(List.of(player));
            given(player.getRoomSession()).willReturn(room);
            given(playerRepository.findAllByRoomSession(room))
                    .willReturn(List.of(player));
            given(player.getPlayerName()).willReturn("씨발");
            given(playerNameGenerator.generate(Set.of("씨발")))
                    .willReturn("빠른여우");

            cleanupService.cleanupBlockedNicknames();

            then(player).should().updatePlayerName("빠른여우");
        }

        @Test
        void 동일_BLOCKED_닉네임을_가진_여러_플레이어를_모두_교체한다() {
            RoomEntity room1 = mock(RoomEntity.class);
            RoomEntity room2 = mock(RoomEntity.class);
            PlayerEntity player1 = mock(PlayerEntity.class);
            PlayerEntity player2 = mock(PlayerEntity.class);

            given(auditRepository.findPlayerNamesByStatus(PlayerNameAuditStatus.BLOCKED))
                    .willReturn(Set.of("씨발"));
            given(dashboardRepository.findTopWinnersBetween(any(), any(), anyInt()))
                    .willReturn(List.of(new TopWinnerResponse("씨발", 2L)));
            given(dashboardRepository.findRacingGameTopPlayers(any(), any(), anyInt()))
                    .willReturn(List.of());
            given(playerRepository.findAllByPlayerName("씨발"))
                    .willReturn(List.of(player1, player2));
            given(player1.getRoomSession()).willReturn(room1);
            given(player2.getRoomSession()).willReturn(room2);
            given(playerRepository.findAllByRoomSession(room1)).willReturn(List.of(player1));
            given(playerRepository.findAllByRoomSession(room2)).willReturn(List.of(player2));
            given(player1.getPlayerName()).willReturn("씨발");
            given(player2.getPlayerName()).willReturn("씨발");
            given(playerNameGenerator.generate(any())).willReturn("빠른여우", "용감한호랑이");

            cleanupService.cleanupBlockedNicknames();

            then(player1).should().updatePlayerName(anyString());
            then(player2).should().updatePlayerName(anyString());
        }

        @Test
        void 이번달_기준으로_랭킹을_조회한다() {
            LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

            given(auditRepository.findPlayerNamesByStatus(PlayerNameAuditStatus.BLOCKED))
                    .willReturn(Set.of("씨발"));
            given(dashboardRepository.findTopWinnersBetween(any(), any(), anyInt()))
                    .willReturn(List.of());
            given(dashboardRepository.findRacingGameTopPlayers(any(), any(), anyInt()))
                    .willReturn(List.of());

            cleanupService.cleanupBlockedNicknames();

            then(dashboardRepository).should()
                    .findTopWinnersBetween(eq(startOfMonth), eq(now), anyInt());
            then(dashboardRepository).should()
                    .findRacingGameTopPlayers(eq(startOfMonth), eq(now), anyInt());
        }
    }
}
