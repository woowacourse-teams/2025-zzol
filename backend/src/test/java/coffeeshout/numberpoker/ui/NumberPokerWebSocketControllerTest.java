package coffeeshout.numberpoker.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.numberpoker.infra.messaging.event.FoldCommandEvent;
import coffeeshout.numberpoker.infra.messaging.event.ReadyCommandEvent;
import coffeeshout.numberpoker.infra.messaging.event.SettingsCommandEvent;
import coffeeshout.numberpoker.ui.command.SettingsCommand;
import java.security.Principal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NumberPokerWebSocketControllerTest {

    @Mock
    StreamPublisher streamPublisher;

    @InjectMocks
    NumberPokerWebSocketController controller;

    Principal principal(String joinCode, String playerName) {
        return () -> joinCode + ":" + playerName;
    }

    @Nested
    class fold {

        @Test
        void 유효한_Principal이면_FoldCommandEvent를_스트림에_발행한다() {
            ArgumentCaptor<FoldCommandEvent> captor = ArgumentCaptor.forClass(FoldCommandEvent.class);

            controller.fold("ABC123", principal("ABC123", "꾹이"));

            verify(streamPublisher).publish(eq(StreamKey.NUMBER_POKER_EVENTS), captor.capture());
            FoldCommandEvent event = captor.getValue();
            assertThat(event.joinCode()).isEqualTo("ABC123");
            assertThat(event.playerName()).isEqualTo("꾹이");
        }

        @Test
        void Principal이_null이면_이벤트를_발행하지_않는다() {
            controller.fold("ABC123", null);

            verify(streamPublisher, never()).publish(any(), any());
        }

        @Test
        void Principal이_PlayerKey_형식이_아니면_이벤트를_발행하지_않는다() {
            controller.fold("ABC123", () -> "sessionId-without-colon");

            verify(streamPublisher, never()).publish(any(), any());
        }

        @Test
        void Principal의_joinCode가_URL_joinCode와_다르면_이벤트를_발행하지_않는다() {
            controller.fold("ABC123", principal("OTHER1", "꾹이"));

            verify(streamPublisher, never()).publish(any(), any());
        }
    }

    @Nested
    class ready {

        @Test
        void 유효한_Principal이면_ReadyCommandEvent를_스트림에_발행한다() {
            ArgumentCaptor<ReadyCommandEvent> captor = ArgumentCaptor.forClass(ReadyCommandEvent.class);

            controller.ready("ABC123", principal("ABC123", "꾹이"));

            verify(streamPublisher).publish(eq(StreamKey.NUMBER_POKER_EVENTS), captor.capture());
            ReadyCommandEvent event = captor.getValue();
            assertThat(event.joinCode()).isEqualTo("ABC123");
            assertThat(event.playerName()).isEqualTo("꾹이");
        }

        @Test
        void Principal이_null이면_이벤트를_발행하지_않는다() {
            controller.ready("ABC123", null);

            verify(streamPublisher, never()).publish(any(), any());
        }

        @Test
        void Principal이_PlayerKey_형식이_아니면_이벤트를_발행하지_않는다() {
            controller.ready("ABC123", () -> "invalid-principal");

            verify(streamPublisher, never()).publish(any(), any());
        }

        @Test
        void Principal의_joinCode가_URL_joinCode와_다르면_이벤트를_발행하지_않는다() {
            controller.ready("ABC123", principal("OTHER1", "꾹이"));

            verify(streamPublisher, never()).publish(any(), any());
        }
    }

    @Nested
    class settings {

        @Test
        void 유효한_Principal이면_SettingsCommandEvent를_스트림에_발행한다() {
            ArgumentCaptor<SettingsCommandEvent> captor = ArgumentCaptor.forClass(SettingsCommandEvent.class);

            controller.settings("ABC123", new SettingsCommand("ignored", 5), principal("ABC123", "꾹이"));

            verify(streamPublisher).publish(eq(StreamKey.NUMBER_POKER_EVENTS), captor.capture());
            SettingsCommandEvent event = captor.getValue();
            assertThat(event.joinCode()).isEqualTo("ABC123");
            assertThat(event.hostName()).isEqualTo("꾹이");
            assertThat(event.roundCount()).isEqualTo(5);
        }

        @Test
        void 페이로드의_hostName은_무시되고_Principal의_playerName이_사용된다() {
            ArgumentCaptor<SettingsCommandEvent> captor = ArgumentCaptor.forClass(SettingsCommandEvent.class);

            controller.settings("ABC123", new SettingsCommand("임포스터", 3), principal("ABC123", "꾹이"));

            verify(streamPublisher).publish(eq(StreamKey.NUMBER_POKER_EVENTS), captor.capture());
            assertThat(captor.getValue().hostName()).isEqualTo("꾹이");
        }

        @Test
        void Principal이_null이면_이벤트를_발행하지_않는다() {
            controller.settings("ABC123", new SettingsCommand("꾹이", 3), null);

            verify(streamPublisher, never()).publish(any(), any());
        }

        @Test
        void Principal이_PlayerKey_형식이_아니면_이벤트를_발행하지_않는다() {
            controller.settings("ABC123", new SettingsCommand("꾹이", 3), () -> "rawSessionId");

            verify(streamPublisher, never()).publish(any(), any());
        }

        @Test
        void Principal의_joinCode가_URL_joinCode와_다르면_이벤트를_발행하지_않는다() {
            controller.settings("ABC123", new SettingsCommand("꾹이", 3), principal("OTHER1", "꾹이"));

            verify(streamPublisher, never()).publish(any(), any());
        }
    }
}
