package coffeeshout.room.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.ui.request.RoomEnterRequest;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import coffeeshout.room.ui.response.GuestNameExistResponse;
import coffeeshout.room.ui.response.JoinCodeExistResponse;
import coffeeshout.room.ui.response.RemainingMiniGameResponse;
import coffeeshout.room.ui.response.RoomCreateResponse;
import coffeeshout.room.ui.response.RoomEnterResponse;
import coffeeshout.support.test.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("RoomRestController 통합 테스트")
class RoomRestControllerTest {

    private static final String INVALID_JOIN_CODE = "XXXX";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RoomRepository roomRepository;

    @Nested
    @DisplayName("방 생성 테스트")
    class CreateRoomTest {

        @Test
        void 정상적인_방_생성_요청_시_방이_생성되고_응답을_반환한다() throws Exception {
            // given
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest request = new RoomEnterRequest("테스트유저", menuRequest);

            // when & then
            String response = mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.joinCode").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomCreateResponse roomCreateResponse = objectMapper.readValue(response, RoomCreateResponse.class);
            assertThat(roomCreateResponse.joinCode()).isNotBlank();
        }

        @Test
        void 플레이어_이름이_없는_경우_400_에러를_반환한다() throws Exception {
            // given
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest request = new RoomEnterRequest(null, menuRequest);

            // when & then
            mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("방 입장 Validation 테스트")
    class EnterRoomValidationTest {

        @ParameterizedTest
        @NullAndEmptySource
        void 플레이어_이름이_없거나_빈값인_경우_400_에러를_반환한다(String invalidPlayerName) throws Exception {
            // given
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest request = new RoomEnterRequest(invalidPlayerName, menuRequest);

            // when & then - Validation 실패는 비동기 처리 전에 발생
            mockMvc.perform(post("/rooms/{joinCode}", "ANYCODE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 메뉴_정보가_없는_경우_400_에러를_반환한다() throws Exception {
            // given
            RoomEnterRequest request = new RoomEnterRequest("게스트", null);

            // when & then - Validation 실패는 비동기 처리 전에 발생
            mockMvc.perform(post("/rooms/{joinCode}", "ANYCODE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("방 입장 비즈니스 로직 테스트")
    class EnterRoomBusinessTest {

        @Test
        void 존재하는_방에_정상적으로_입장할_수_있다() throws Exception {
            // given - 먼저 방을 생성
            SelectedMenuRequest hostMenuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest createRequest = new RoomEnterRequest("호스트", hostMenuRequest);

            String createResponse = mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomCreateResponse roomCreateResponse = objectMapper.readValue(createResponse, RoomCreateResponse.class);
            String joinCode = roomCreateResponse.joinCode();

            // given - 게스트 입장 요청
            SelectedMenuRequest guestMenuRequest = new SelectedMenuRequest(2L, "라떼", MenuTemperature.ICE);
            RoomEnterRequest enterRequest = new RoomEnterRequest("게스트", guestMenuRequest);

            // when & then - 비동기 테스트
            var result = mockMvc.perform(post("/rooms/{joinCode}", joinCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(enterRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            String enterResponse = mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.joinCode").value(joinCode))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomEnterResponse roomEnterResponse = objectMapper.readValue(enterResponse, RoomEnterResponse.class);
            Room room = roomRepository.findByJoinCode(new JoinCode(joinCode)).get();

            assertThat(roomEnterResponse.joinCode()).isEqualTo(joinCode);
            assertThat(room.getPlayers())
                    .extracting(player -> player.getName().value())
                    .containsExactlyInAnyOrder("호스트", "게스트");
            assertThat(room.getPlayers()).hasSize(2);
        }

        @Test
        void 존재하지_않는_방_코드로_입장_시도_시_404_에러를_반환한다() throws Exception {
            // given
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest request = new RoomEnterRequest("테스트유저", menuRequest);

            // when & then - 비동기 테스트
            var result = mockMvc.perform(post("/rooms/{joinCode}", INVALID_JOIN_CODE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isNotFound());
        }

        @Test
        void 중복된_플레이어_이름으로_입장_시도_시_409_에러를_반환한다() throws Exception {
            // given - 먼저 방을 생성
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest createRequest = new RoomEnterRequest("호스트", menuRequest);

            String createResponse = mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomCreateResponse roomCreateResponse = objectMapper.readValue(createResponse, RoomCreateResponse.class);
            String joinCode = roomCreateResponse.joinCode();

            // given - 중복된 플레이어 이름으로 입장 요청
            SelectedMenuRequest guestMenuRequest = new SelectedMenuRequest(2L, "라떼", MenuTemperature.ICE);
            RoomEnterRequest enterRequest = new RoomEnterRequest("호스트", guestMenuRequest);

            // when & then - 비동기 테스트
            var result = mockMvc.perform(post("/rooms/{joinCode}", joinCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(enterRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isConflict());
        }


        @Test
        void 방이_가득_찬_경우_입장_시도_시_409_에러를_반환한다() throws Exception {
            // given - 먼저 방을 생성
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest createRequest = new RoomEnterRequest("호스트", menuRequest);

            String createResponse = mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomCreateResponse roomCreateResponse = objectMapper.readValue(createResponse, RoomCreateResponse.class);
            String joinCode = roomCreateResponse.joinCode();

            // given - 8명의 게스트 입장 (호스트 1명 + 게스트 8명 = 총 9명)
            for (int i = 1; i <= 8; i++) {

                RoomEnterRequest enterRequest = new RoomEnterRequest("게스트" + i, menuRequest);

                var result = mockMvc.perform(post("/rooms/{joinCode}", joinCode)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(enterRequest)))
                        .andExpect(request().asyncStarted())
                        .andReturn();

                mockMvc.perform(asyncDispatch(result))
                        .andExpect(status().isOk());
            }

            // given - 9번째 게스트 입장 시도 (정원 초과: 호스트 1명 + 게스트 9명 = 총 10명)
            RoomEnterRequest overflowRequest = new RoomEnterRequest("초과유저", menuRequest);

            // when & then - 비동기 테스트
            var result = mockMvc.perform(post("/rooms/{joinCode}", joinCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(overflowRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("방 코드 존재 확인 테스트")
    class CheckJoinCodeTest {

        @Test
        void 존재하는_방_코드_확인_시_true를_반환한다() throws Exception {
            // given - 방 생성
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest request = new RoomEnterRequest("테스트유저", menuRequest);

            String createResponse = mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomCreateResponse roomCreateResponse = objectMapper.readValue(createResponse, RoomCreateResponse.class);
            String joinCode = roomCreateResponse.joinCode();

            // when & then
            String response = mockMvc.perform(get("/rooms/check-joinCode")
                            .param("joinCode", joinCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exist").value(true))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JoinCodeExistResponse joinCodeExistResponse = objectMapper.readValue(response, JoinCodeExistResponse.class);
            assertThat(joinCodeExistResponse.exist()).isTrue();
        }

        @Test
        void 존재하지_않는_방_코드_확인_시_false를_반환한다() throws Exception {
            // when & then
            String response = mockMvc.perform(get("/rooms/check-joinCode")
                            .param("joinCode", INVALID_JOIN_CODE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exist").value(false))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JoinCodeExistResponse joinCodeExistResponse = objectMapper.readValue(response, JoinCodeExistResponse.class);
            assertThat(joinCodeExistResponse.exist()).isFalse();
        }
    }

    @Nested
    @DisplayName("게스트 이름 중복 확인 테스트")
    class CheckGuestNameTest {

        @Test
        void 중복되지_않는_게스트_이름_확인_시_false를_반환한다() throws Exception {
            // given - 방 생성
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest request = new RoomEnterRequest("호스트", menuRequest);

            String createResponse = mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomCreateResponse roomCreateResponse = objectMapper.readValue(createResponse, RoomCreateResponse.class);
            String joinCode = roomCreateResponse.joinCode();

            // when & then
            String response = mockMvc.perform(get("/rooms/check-guestName")
                            .param("joinCode", joinCode)
                            .param("guestName", "게스트"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exist").value(false))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            GuestNameExistResponse guestNameExistResponse = objectMapper.readValue(response,
                    GuestNameExistResponse.class);
            assertThat(guestNameExistResponse.exist()).isFalse();
        }

        @Test
        void 중복되는_게스트_이름_확인_시_true를_반환한다() throws Exception {
            // given - 방 생성
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest request = new RoomEnterRequest("호스트", menuRequest);

            String createResponse = mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomCreateResponse roomCreateResponse = objectMapper.readValue(createResponse, RoomCreateResponse.class);
            String joinCode = roomCreateResponse.joinCode();

            // when & then - 동일한 이름으로 중복 확인
            String response = mockMvc.perform(get("/rooms/check-guestName")
                            .param("joinCode", joinCode)
                            .param("guestName", "호스트"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exist").value(true))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            GuestNameExistResponse guestNameExistResponse = objectMapper.readValue(response,
                    GuestNameExistResponse.class);
            assertThat(guestNameExistResponse.exist()).isTrue();
        }
    }

    @Nested
    @DisplayName("미니게임 조회 테스트")
    class MiniGameTest {

        @Test
        void 전체_미니게임_목록을_조회할_수_있다() throws Exception {
            // when & then
            String response = mockMvc.perform(get("/rooms/minigames"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            List<MiniGameType> miniGameTypes = objectMapper.readValue(response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MiniGameType.class));

            assertThat(miniGameTypes).isNotEmpty()
                    .contains(MiniGameType.CARD_GAME);
        }

        @Test
        void 특정_방의_선택된_미니게임_목록을_조회할_수_있다() throws Exception {
            // given - 방 생성
            SelectedMenuRequest menuRequest = new SelectedMenuRequest(1L, "아메리카노", MenuTemperature.HOT);
            RoomEnterRequest request = new RoomEnterRequest("호스트", menuRequest);

            String createResponse = mockMvc.perform(post("/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RoomCreateResponse roomCreateResponse = objectMapper.readValue(createResponse, RoomCreateResponse.class);
            String joinCode = roomCreateResponse.joinCode();

            // when & then
            String response = mockMvc.perform(get("/rooms/minigames/selected")
                            .param("joinCode", joinCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            List<MiniGameType> selectedGames = objectMapper.readValue(response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MiniGameType.class));

            assertThat(selectedGames).isNotNull();
        }

        @Test
        void 존재하지_않는_방의_선택된_미니게임_조회_시_404_에러를_반환한다() throws Exception {
            // when & then
            mockMvc.perform(get("/rooms/minigames/selected")
                            .param("joinCode", INVALID_JOIN_CODE))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void 현재_남은_게임을_조회한다() throws Exception {
        // given
        Room 호스트_꾹이 = RoomFixture.호스트_꾹이();
        roomRepository.save(호스트_꾹이);
        호스트_꾹이.addMiniGame(호스트_꾹이.getHost().getName(), new CardGame(new CardGameRandomDeckGenerator(), 0));
        호스트_꾹이.addMiniGame(호스트_꾹이.getHost().getName(), new RacingGame());

        // when
        var remainingMiniGamesResponse = objectMapper.readValue(mockMvc.perform(
                        (get("/rooms/{joinCode}/miniGames/remaining", 호스트_꾹이.getJoinCode())
                                .contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(), RemainingMiniGameResponse.class);

        // then
        assertThat(remainingMiniGamesResponse.remaining()).containsExactly("CARD_GAME", "RACING_GAME");
    }
}
