package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NoOpNicknameAuditorTest {

    private NoOpNicknameAuditor auditor;

    @BeforeEach
    void setUp() {
        auditor = new NoOpNicknameAuditor();
    }

    @Nested
    class audit_검열 {

        @Test
        void 모든_닉네임을_CLEAN으로_반환한다() {
            final var results = auditor.audit(List.of("용감한호랑이", "씨발", "닉네임"));

            assertThat(results).allMatch(r -> r.status() == NicknameAuditStatus.CLEAN);
        }

        @Test
        void 입력과_동일한_수의_결과를_반환한다() {
            final var results = auditor.audit(List.of("닉네임1", "닉네임2", "닉네임3"));

            assertThat(results).hasSize(3);
        }

        @Test
        void null_입력은_예외가_발생한다() {
            assertThatThrownBy(() -> auditor.audit(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void 빈_리스트는_빈_결과를_반환한다() {
            assertThat(auditor.audit(List.of())).isEmpty();
        }
    }
}
