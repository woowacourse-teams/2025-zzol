package coffeeshout.sandbox;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RemediationE2eSandboxTest {

    private final RemediationE2eSandbox sandbox = new RemediationE2eSandbox();

    @Test
    void lengthOf_shouldReturnCorrectLengthForNonNullString() {
        assertEquals(5, sandbox.lengthOf("hello"));
    }

    @Test
    void lengthOf_shouldReturnZeroForNullInput() {
        // 수정 전: 이 테스트는 NullPointerException을 발생시켰습니다.
        // 수정 후: 이 테스트는 0을 반환하며 통과해야 합니다.
        assertEquals(0, sandbox.lengthOf(null));
    }

    @Test
    void lengthOf_shouldReturnZeroForEmptyString() {
        assertEquals(0, sandbox.lengthOf(""));
    }
}