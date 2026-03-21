package coffeeshout.room.application.service.nickname;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import coffeeshout.fixture.NicknameAuditFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.audit.NicknameAuditResult;
import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.domain.audit.NicknameAuditor;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityEntity;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityJpaRepository;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditJpaRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NicknameAuditBatchProcessorTest extends ServiceTest {

    @MockitoBean NicknameAuditor nicknameAuditor;

    @Autowired NicknameAuditBatchProcessor batchProcessor;
    @Autowired NicknameAuditJpaRepository auditRepository;
    @Autowired CustomProfanityJpaRepository customProfanityRepository;
    @Autowired BadWordFiltering badWordFiltering;
    @Autowired MeterRegistry meterRegistry;

    private static final double THRESHOLD = 0.85;

    private NicknameAuditResult flaggedResult(String nickname) {
        return NicknameAuditResult.of(nickname, true, 0.97, "욕설", THRESHOLD);
    }

    private NicknameAuditResult pendingResult(String nickname) {
        return NicknameAuditResult.of(nickname, true, 0.60, "애매함", THRESHOLD);
    }

    private NicknameAuditResult cleanResult(String nickname) {
        return NicknameAuditResult.of(nickname, false, 0.99, "정상", THRESHOLD);
    }

    @Nested
    class FLAGGED_자동_차단 {

        @Test
        void 기준치_이상이면_custom_profanity에_등록된다() {
            final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("욕설닉네임"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

            batchProcessor.process(List.of(entity));

            assertThat(customProfanityRepository.existsByWord("욕설닉네임")).isTrue();
        }

        @Test
        void 기준치_이상이면_BadWordFiltering에_즉시_반영된다() {
            final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("욕설닉네임"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

            batchProcessor.process(List.of(entity));

            assertThat(badWordFiltering.check("욕설닉네임")).isTrue();
        }

        @Test
        void 엔티티_상태가_FLAGGED로_변경된다() {
            final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("욕설닉네임"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

            batchProcessor.process(List.of(entity));

            assertThat(auditRepository.findById(entity.getId()).orElseThrow().getStatus())
                    .isEqualTo(NicknameAuditStatus.FLAGGED);
        }

        @Nested
        class 이미_custom_profanity에_등록된_경우 {

            @Test
            void 중복_등록하지_않는다() {
                customProfanityRepository.save(
                        new CustomProfanityEntity("욕설닉네임", CustomProfanityEntity.Source.OPERATOR_MANUAL)
                );
                final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("욕설닉네임"));
                given(nicknameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

                batchProcessor.process(List.of(entity));

                assertThat(customProfanityRepository.findWords(Pageable.unpaged()))
                        .containsExactly("욕설닉네임");
            }
        }
    }

    @Nested
    class 비_FLAGGED_결과 {

        @Test
        void PENDING이면_custom_profanity에_등록하지_않는다() {
            final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("애매한닉네임"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(pendingResult("애매한닉네임")));

            batchProcessor.process(List.of(entity));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(customProfanityRepository.existsByWord("애매한닉네임")).isFalse();
                softly.assertThat(auditRepository.findById(entity.getId()).orElseThrow().getStatus())
                        .isEqualTo(NicknameAuditStatus.PENDING);
            });
        }

        @Test
        void CLEAN이면_custom_profanity에_등록하지_않는다() {
            final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("용감한호랑이"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(cleanResult("용감한호랑이")));

            batchProcessor.process(List.of(entity));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(customProfanityRepository.existsByWord("용감한호랑이")).isFalse();
                softly.assertThat(auditRepository.findById(entity.getId()).orElseThrow().getStatus())
                        .isEqualTo(NicknameAuditStatus.CLEAN);
            });
        }
    }

    @Nested
    class 배치_파싱_실패 {

        @Test
        void auditor가_빈_리스트를_반환하면_배치를_skip하고_0을_반환한다() {
            final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("닉네임"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of());

            final int processed = batchProcessor.process(List.of(entity));

            assertThat(processed).isZero();
        }

        @Test
        void 배치_skip_시_엔티티_상태가_UNAUDITED로_유지된다() {
            final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("닉네임"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of());

            batchProcessor.process(List.of(entity));

            assertThat(auditRepository.findById(entity.getId()).orElseThrow().getStatus())
                    .isEqualTo(NicknameAuditStatus.UNAUDITED);
        }

        @Test
        void 배치_skip_카운터가_증가한다() {
            final NicknameAuditEntity entity = auditRepository.save(NicknameAuditFixture.검열대기("닉네임"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of());
            final double before = meterRegistry.counter("nickname.audit.batch.skipped").count();

            batchProcessor.process(List.of(entity));

            assertThat(meterRegistry.counter("nickname.audit.batch.skipped").count()).isEqualTo(before + 1);
        }
    }

    @Nested
    class resultMap에_없는_엔티티 {

        @Test
        void 해당_엔티티는_상태_변경_없이_skip된다() {
            final NicknameAuditEntity matched = auditRepository.save(NicknameAuditFixture.검열대기("욕설닉네임"));
            final NicknameAuditEntity unmatched = auditRepository.save(NicknameAuditFixture.검열대기("다른닉네임"));
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

            batchProcessor.process(List.of(matched, unmatched));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(auditRepository.findById(matched.getId()).orElseThrow().getStatus())
                        .isEqualTo(NicknameAuditStatus.FLAGGED);
                softly.assertThat(auditRepository.findById(unmatched.getId()).orElseThrow().getStatus())
                        .isEqualTo(NicknameAuditStatus.UNAUDITED);
            });
        }
    }
}
