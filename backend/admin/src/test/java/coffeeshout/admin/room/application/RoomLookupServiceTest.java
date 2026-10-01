package coffeeshout.admin.room.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.admin.room.application.RoomLookupService.RoomDetail;
import coffeeshout.admin.room.domain.RoomLookupRepository;
import coffeeshout.admin.room.domain.RoomMiniGameResult;
import coffeeshout.admin.room.domain.RoomPlayer;
import coffeeshout.admin.room.domain.RoomRouletteResult;
import coffeeshout.admin.room.domain.RoomSummary;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@DisplayName("RoomLookupService")
@ExtendWith(MockitoExtension.class)
class RoomLookupServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 6, 12, 0);

    @Mock
    private RoomLookupRepository roomLookupRepository;

    @InjectMocks
    private RoomLookupService roomLookupService;

    private static RoomSummary summary(Long id) {
        return new RoomSummary(id, "ABCDE", RoomState.DONE, CREATED_AT, CREATED_AT.plusMinutes(8), 4);
    }

    @Nested
    class search {

        @Test
        void 페이지_크기를_고정해_조회한다() {
            given(roomLookupRepository.search(any(), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

            roomLookupService.search("ABCDE", 3);

            final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            then(roomLookupRepository).should().search(any(), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(3);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        }

        @Test
        void 같은_코드의_방이_여러_개면_모두_돌려준다() {
            // join_code 는 유니크가 아니다. 시간이 지나 같은 코드가 다시 쓰인다.
            given(roomLookupRepository.search(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(summary(2L), summary(1L))));

            assertThat(roomLookupService.search("ABCDE", 0).getContent())
                    .extracting(RoomSummary::id)
                    .containsExactly(2L, 1L);
        }
    }

    @Nested
    class findDetail {

        @Test
        void 방과_참여자와_게임결과와_룰렛을_모아_돌려준다() {
            given(roomLookupRepository.findSummaryById(1L)).willReturn(Optional.of(summary(1L)));
            given(roomLookupRepository.findPlayers(1L))
                    .willReturn(List.of(new RoomPlayer(10L, "엠제이", PlayerType.HOST, 100L, "mj", "AB12C", CREATED_AT)));
            given(roomLookupRepository.findMiniGameResults(1L))
                    .willReturn(
                            List.of(new RoomMiniGameResult(MiniGameType.RACING_GAME, 10L, "엠제이", 1, 500L, CREATED_AT)));
            given(roomLookupRepository.findRouletteResult(1L))
                    .willReturn(Optional.of(new RoomRouletteResult(10L, "엠제이", 12, CREATED_AT.plusMinutes(8))));

            final RoomDetail detail = roomLookupService.findDetail(1L);

            assertThat(detail.summary().joinCode()).isEqualTo("ABCDE");
            assertThat(detail.players()).hasSize(1);
            assertThat(detail.miniGameResults()).hasSize(1);
            assertThat(detail.rouletteResult()).isPresent();
        }

        @Test
        void 룰렛까지_못_간_방은_룰렛이_비어_있다() {
            // 중도 이탈은 정상 경로다. 예외가 아니다.
            given(roomLookupRepository.findSummaryById(1L)).willReturn(Optional.of(summary(1L)));
            given(roomLookupRepository.findPlayers(1L)).willReturn(List.of());
            given(roomLookupRepository.findMiniGameResults(1L)).willReturn(List.of());
            given(roomLookupRepository.findRouletteResult(1L)).willReturn(Optional.empty());

            assertThat(roomLookupService.findDetail(1L).rouletteResult()).isEmpty();
        }

        @Test
        void 없는_방이면_거부하고_나머지를_조회하지_않는다() {
            given(roomLookupRepository.findSummaryById(404L)).willReturn(Optional.empty());

            assertCoffeeShoutException(() -> roomLookupService.findDetail(404L), GlobalErrorCode.NOT_EXIST);
            then(roomLookupRepository).should(never()).findPlayers(any());
            then(roomLookupRepository).should(never()).findMiniGameResults(any());
        }
    }
}
