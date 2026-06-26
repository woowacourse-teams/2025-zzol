package coffeeshout.zzolbot.remediation.agent;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import org.junit.jupiter.api.Test;

class RemediationAgentMainTest {

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
