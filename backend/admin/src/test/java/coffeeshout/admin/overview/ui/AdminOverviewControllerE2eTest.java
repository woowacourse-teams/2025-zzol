package coffeeshout.admin.overview.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.admin.support.AdminApiE2eTest;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("홈 요약 API")
class AdminOverviewControllerE2eTest extends AdminApiE2eTest {

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    private void givenFinishedRoom() {
        final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
        room.updateRoomStatus(RoomState.DONE);
        roomJpaRepository.saveAndFlush(room);
        playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST, null));
        playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST, null));
    }

    @Nested
    class 처리_대기 {

        @Test
        void 다섯_큐를_한_번에_돌려준다() throws Exception {
            // 화면이 큐마다 따로 물어보면 다섯 번 왕복한다.
            mockMvc.perform(get("/admin/api/overview/action-queue").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingReports").exists())
                    .andExpect(jsonPath("$.flaggedNicknames").exists())
                    .andExpect(jsonPath("$.pendingNicknames").exists())
                    .andExpect(jsonPath("$.blockedIps").exists())
                    .andExpect(jsonPath("$.deadLetters").exists());
        }
    }

    @Nested
    class 추이 {

        @Test
        void 요청한_날짜_수만큼_점을_준다() throws Exception {
            // 값이 없는 날도 0으로 채워야 화면이 이웃한 두 점을 이어 붙이지 않는다.
            mockMvc.perform(get("/admin/api/overview/trend").param("days", "14").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(14));
        }

        @Test
        void 기간_상한을_넘기면_400_이다() throws Exception {
            mockMvc.perform(get("/admin/api/overview/trend")
                            .param("days", "365")
                            .with(admin()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class 오늘과_기간 {

        @Test
        void 오늘_요약을_돌려준다() throws Exception {
            givenFinishedRoom();

            mockMvc.perform(get("/admin/api/overview/summary").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.funnel.created").value(1))
                    .andExpect(jsonPath("$.players").value(2));
        }

        @Test
        void 날짜를_주면_그날을_돌려준다() throws Exception {
            mockMvc.perform(get("/admin/api/overview/summary")
                            .param("date", "2020-01-01")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").value("2020-01-01"))
                    .andExpect(jsonPath("$.funnel.created").value(0));
        }

        @Test
        void 기간_합계는_퍼널로_돌려준다() throws Exception {
            givenFinishedRoom();

            mockMvc.perform(get("/admin/api/overview/period")
                            .param("days", "30")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.funnel.created").value(1))
                    .andExpect(jsonPath("$.funnel.completed").value(1));
        }
    }

    @Nested
    class 게임별 {

        @Test
        void 완료된_게임이_없으면_빈_목록이다() throws Exception {
            mockMvc.perform(get("/admin/api/overview/games").param("days", "30").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Test
    void 토큰이_없으면_401_이다() throws Exception {
        mockMvc.perform(get("/admin/api/overview/action-queue")).andExpect(status().isUnauthorized());
    }
}
