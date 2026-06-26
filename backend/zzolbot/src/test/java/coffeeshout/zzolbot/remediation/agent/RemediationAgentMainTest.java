package coffeeshout.zzolbot.remediation.agent;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import org.junit.jupiter.api.Test;

class RemediationAgentMainTest {

    private DefectLocation roomLocation() {
        return new DefectLocation("coffeeshout.room.application.RoomService", "find",
                "backend/room/src/main/java/coffeeshout/room/application/RoomService.java", 42, ":room");
    }

    private PatchProposal proposalWithTest(String reproTestPath, String reproTestSource) {
        return new PatchProposal("target.java", "src", reproTestPath, reproTestSource, "이유");
    }

    @Test
    void 테스트_경로를_소스의_package_class로_대상모듈에_정규화한다() {
        final PatchProposal proposal = proposalWithTest(
                "RoomServiceTest.java",
                "package coffeeshout.room.application;\n\nclass RoomServiceTest {}");

        final String[] normalized = RemediationAgentMain.normalizeTestLocation(roomLocation(), proposal);

        assertThat(normalized[0])
                .isEqualTo("backend/room/src/test/java/coffeeshout/room/application/RoomServiceTest.java");
        assertThat(normalized[1]).isEqualTo("coffeeshout.room.application.RoomServiceTest");
    }

    @Test
    void 소스에서_class를_못_찾으면_LLM_경로로_폴백한다() {
        final PatchProposal proposal = proposalWithTest(
                "backend/room/src/test/java/coffeeshout/room/FooTest.java", "// 깨진 소스");

        final String[] normalized = RemediationAgentMain.normalizeTestLocation(roomLocation(), proposal);

        assertThat(normalized[0]).isEqualTo("backend/room/src/test/java/coffeeshout/room/FooTest.java");
        assertThat(normalized[1]).isEqualTo("coffeeshout.room.FooTest");
    }

    @Test
    void 테스트_경로에서_클래스_FQN을_도출한다() {
        final String path = "backend/room/src/test/java/coffeeshout/room/RoomServiceTest.java";

        assertThat(RemediationAgentMain.reproTestClass(path)).isEqualTo("coffeeshout.room.RoomServiceTest");
    }

    @Test
    void 테스트_경로_형식이_아니면_빈문자열을_반환한다() {
        assertThat(RemediationAgentMain.reproTestClass("not/a/test/path.txt")).isEmpty();
    }

    @Test
    void 알_수_없는_결함유형은_UNKNOWN으로_파싱한다() {
        assertThat(RemediationAgentMain.parseDefectType("NULL_POINTER")).isEqualTo(DefectType.NULL_POINTER);
        assertThat(RemediationAgentMain.parseDefectType("WEIRD")).isEqualTo(DefectType.UNKNOWN);
    }
}
