package coffeeshout.zzolbot.remediation.agent;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class GeminiCodingAgentTest {

    private final GeminiCodingAgent agent = new GeminiCodingAgent(null, "gemini-2.5-flash", new ObjectMapper());

    private DefectContext context() {
        final DefectLocation loc = new DefectLocation(
                "coffeeshout.room.application.RoomService", "find",
                "backend/room/src/main/java/coffeeshout/room/application/RoomService.java", 42, ":room");
        return new DefectContext(DefectType.NULL_POINTER, "NPE", "stack", loc, "class C {}");
    }

    @Test
    void JSON_응답을_PatchProposal로_파싱한다() {
        final String json = """
                {
                  "modifiedSource": "package a; class C {}",
                  "reproTestPath": "backend/room/src/test/java/coffeeshout/room/RoomServiceTest.java",
                  "reproTestSource": "class RoomServiceTest {}",
                  "rationale": "널 가드 추가"
                }""";

        final PatchProposal proposal = agent.parse(context(), json);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(proposal.targetPath())
                    .isEqualTo("backend/room/src/main/java/coffeeshout/room/application/RoomService.java");
            softly.assertThat(proposal.modifiedSource()).isEqualTo("package a; class C {}");
            softly.assertThat(proposal.reproTestPath())
                    .isEqualTo("backend/room/src/test/java/coffeeshout/room/RoomServiceTest.java");
            softly.assertThat(proposal.reproTestSource()).isEqualTo("class RoomServiceTest {}");
            softly.assertThat(proposal.rationale()).isEqualTo("널 가드 추가");
        });
    }

    @Test
    void 깨진_JSON은_크래시하지_않고_빈_제안으로_떨군다() {
        final PatchProposal proposal = agent.parse(context(), "{\"modifiedSource\": \"x\" 깨진 응답");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(proposal.modifiedSource()).isEmpty();
            softly.assertThat(proposal.reproTestSource()).isEmpty();
            softly.assertThat(proposal.reproTestPath()).isEmpty();
        });
    }
}
