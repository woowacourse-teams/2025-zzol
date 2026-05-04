package coffeeshout.global.zzolbot.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZzolBotPromptTemplateTest {

    private ZzolBotPromptTemplate promptTemplate;

    @BeforeEach
    void setUp() {
        promptTemplate = new ZzolBotPromptTemplate();
    }

    @Test
    void 핵심_도메인_용어와_도구_설명을_포함한_프롬프트를_반환한다() {
        final String prompt = promptTemplate.build();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(prompt).isNotBlank();
            softly.assertThat(prompt).contains("joinCode");
            softly.assertThat(prompt).contains("ZzolBot");
            softly.assertThat(prompt).contains("room_state");
            softly.assertThat(prompt).contains("outbox_events");
            softly.assertThat(prompt).contains("loki_logs");
            softly.assertThat(prompt).contains("RoomState");
            softly.assertThat(prompt).contains("DEAD_LETTER");
        });
    }
}
