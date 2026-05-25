package coffeeshout.room.application.service.player.name;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.fixture.PlayerNameAuditFixture;
import coffeeshout.support.ServiceTest;
import coffeeshout.room.domain.audit.PlayerNameAuditResult;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.domain.audit.PlayerNameAuditor;
import coffeeshout.global.event.ProfanityWordBlockedEvent;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityJpaRepository;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditJpaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PlayerNameAuditBatchProcessorTest extends ServiceTest {

    @MockitoBean
    PlayerNameAuditor playerNameAuditor;

    @Autowired
    PlayerNameAuditBatchProcessor batchProcessor;
    @Autowired
    PlayerNameAuditJpaRepository auditRepository;
    @Autowired
    CustomProfanityJpaRepository customProfanityRepository;
    @Autowired
    MeterRegistry meterRegistry;

    private static final double THRESHOLD = 0.85;

    private PlayerNameAuditResult flaggedResult(String nickname) {
        return PlayerNameAuditResult.of(nickname, true, 0.97, "욕설", THRESHOLD);
    }

    private PlayerNameAuditResult pendingResult(String nickname) {
        return PlayerNameAuditResult.of(nickname, true, 0.60, "애매함", THRESHOLD);
    }

    private PlayerNameAuditResult cleanResult(String nickname) {
        return PlayerNameAuditResult.of(nickname, false, 0.99, "정상", THRESHOLD);
    }

    @Nested
    class FLAGGED_자동_차단 {

        @Test
        void 기준치_이상이면_custom_profanity에_등록된다() {
            final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("욕설닉네임"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

            batchProcessor.process(List.of(entity));

            assertThat(customProfanityRepository.existsByWord("욕설닉네임")).isTrue();
        }

        @Test
        void 기준치_이상이면_ProfanityWordBlockedEvent를_발행한다() {
            final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("욕설닉네임"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

            batchProcessor.process(List.of(entity));

            verify(eventPublisher).publishEvent(new ProfanityWordBlockedEvent("욕설닉네임"));
        }

        @Test
        void 엔티티_상태가_FLAGGED로_변경된다() {
            final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("욕설닉네임"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

            batchProcessor.process(List.of(entity));

            assertThat(auditRepository.findById(entity.getId()).orElseThrow().getStatus())
                    .isEqualTo(PlayerNameAuditStatus.FLAGGED);
        }

        @Nested
        class 이미_custom_profanity에_등록된_경우 {

            @Test
            void 중복_등록하지_않는다() {
                customProfanityRepository.insertIgnore("욕설닉네임", "OPERATOR_MANUAL");
                final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("욕설닉네임"));
                given(playerNameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

                batchProcessor.process(List.of(entity));

                assertThat(customProfanityRepository.findWords(Pageable.unpaged()).getContent())
                        .containsExactly("욕설닉네임");
            }
        }
    }

    @Nested
    class 비_FLAGGED_결과 {

        @Test
        void PENDING이면_custom_profanity에_등록하지_않는다() {
            final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("애매한닉네임"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of(pendingResult("애매한닉네임")));

            batchProcessor.process(List.of(entity));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(customProfanityRepository.existsByWord("애매한닉네임")).isFalse();
                softly.assertThat(auditRepository.findById(entity.getId()).orElseThrow().getStatus())
                        .isEqualTo(PlayerNameAuditStatus.PENDING);
            });
        }

        @Test
        void CLEAN이면_custom_profanity에_등록하지_않는다() {
            final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("용감한호랑이"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of(cleanResult("용감한호랑이")));

            batchProcessor.process(List.of(entity));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(customProfanityRepository.existsByWord("용감한호랑이")).isFalse();
                softly.assertThat(auditRepository.findById(entity.getId()).orElseThrow().getStatus())
                        .isEqualTo(PlayerNameAuditStatus.CLEAN);
            });
        }
    }

    @Nested
    class 배치_파싱_실패 {

        @Test
        void auditor가_빈_리스트를_반환하면_배치를_skip하고_0을_반환한다() {
            final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("닉네임"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of());

            final int processed = batchProcessor.process(List.of(entity));

            assertThat(processed).isZero();
        }

        @Test
        void 배치_skip_시_엔티티_상태가_UNAUDITED로_유지된다() {
            final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("닉네임"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of());

            batchProcessor.process(List.of(entity));

            assertThat(auditRepository.findById(entity.getId()).orElseThrow().getStatus())
                    .isEqualTo(PlayerNameAuditStatus.UNAUDITED);
        }

        @Test
        void 배치_skip_카운터가_증가한다() {
            final PlayerNameAuditEntity entity = auditRepository.save(PlayerNameAuditFixture.검열대기("닉네임"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of());
            final double before = meterRegistry.counter("nickname.audit.batch.skipped").count();

            batchProcessor.process(List.of(entity));

            assertThat(meterRegistry.counter("nickname.audit.batch.skipped").count()).isEqualTo(before + 1);
        }
    }

    @Nested
    class resultMap에_없는_엔티티 {

        @Test
        void 해당_엔티티는_상태_변경_없이_skip된다() {
            final PlayerNameAuditEntity matched = auditRepository.save(PlayerNameAuditFixture.검열대기("욕설닉네임"));
            final PlayerNameAuditEntity unmatched = auditRepository.save(PlayerNameAuditFixture.검열대기("다른닉네임"));
            given(playerNameAuditor.audit(anyList())).willReturn(List.of(flaggedResult("욕설닉네임")));

            batchProcessor.process(List.of(matched, unmatched));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(auditRepository.findById(matched.getId()).orElseThrow().getStatus())
                        .isEqualTo(PlayerNameAuditStatus.FLAGGED);
                softly.assertThat(auditRepository.findById(unmatched.getId()).orElseThrow().getStatus())
                        .isEqualTo(PlayerNameAuditStatus.UNAUDITED);
            });
        }
    }
}
