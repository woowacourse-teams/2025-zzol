package coffeeshout.minigame.ui.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.minigame.ui.request.command.SelectCardCommand;
import coffeeshout.minigame.ui.request.command.StartMiniGameCommand;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class MiniGameMessageTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void commandType_이_commandRequest_의_타입을_정한다() {
        final MiniGameMessage start = mapper.readValue(
                "{\"commandType\":\"START_MINI_GAME\",\"commandRequest\":{\"hostName\":\"루키\"}}",
                MiniGameMessage.class);
        final MiniGameMessage select = mapper.readValue(
                "{\"commandType\":\"SELECT_CARD\",\"commandRequest\":{\"playerName\":\"루키\",\"cardIndex\":3}}",
                MiniGameMessage.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(start.commandRequest()).isEqualTo(new StartMiniGameCommand("루키"));
            softly.assertThat(select.commandRequest()).isEqualTo(new SelectCardCommand("루키", 3));
        });
    }

    @Test
    void 직렬화하면_commandType_이_봉투에_붙는다() {
        final String json = mapper.writeValueAsString(new MiniGameMessage(new SelectCardCommand("루키", 3)));

        assertThat(mapper.readTree(json).get("commandType").asString()).isEqualTo("SELECT_CARD");
    }

    @Test
    void 모르는_commandType_은_거부한다() {
        assertThatThrownBy(() ->
                        mapper.readValue("{\"commandType\":\"NOPE\",\"commandRequest\":{}}", MiniGameMessage.class))
                .isInstanceOf(JacksonException.class);
    }
}
