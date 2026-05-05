package coffeeshout.global.zzolbot.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.zzolbot.infra.ZzolBotSessionEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FewShotSelectorTest {

    private FewShotSelector selector;

    @BeforeEach
    void setUp() {
        selector = new FewShotSelector();
    }

    private ZzolBotSessionEntity sessionWithId(long id) {
        final ZzolBotSessionEntity entity = ZzolBotSessionEntity.create("질문" + id, "답변" + id, "admin");
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private List<ZzolBotSessionEntity> pool(int size) {
        return IntStream.rangeClosed(1, size)
                .mapToObj(i -> sessionWithId((long) i))
                .toList();
    }

    @Nested
    class select_메서드 {

        @Test
        void pool이_비어_있으면_빈_Selection을_반환한다() {
            final FewShotSelector.Selection selection = selector.select("질문", List.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(selection.examples()).isEmpty();
                softly.assertThat(selection.ids()).isEmpty();
            });
        }

        @Test
        void pool이_5개_미만이면_전체를_반환한다() {
            final List<ZzolBotSessionEntity> smallPool = pool(3);

            final FewShotSelector.Selection selection = selector.select("질문", smallPool);

            assertThat(selection.examples()).hasSize(3);
        }

        @Test
        void pool이_5개_이상이면_최대_5개를_반환한다() {
            final List<ZzolBotSessionEntity> largePool = pool(20);

            final FewShotSelector.Selection selection = selector.select("질문", largePool);

            assertThat(selection.examples()).hasSize(5);
        }

        @Test
        void 동일한_question과_pool이면_항상_동일한_ids를_반환한다() {
            final List<ZzolBotSessionEntity> largePool = pool(20);
            final String question = "A4BX 방 상태 알려줘";

            final FewShotSelector.Selection first = selector.select(question, largePool);

            for (int i = 0; i < 100; i++) {
                final FewShotSelector.Selection repeated = selector.select(question, new ArrayList<>(largePool));
                assertThat(repeated.ids()).isEqualTo(first.ids());
            }
        }

        @Test
        void 다른_question이면_다른_ids를_반환할_수_있다() {
            final List<ZzolBotSessionEntity> largePool = pool(20);

            final FewShotSelector.Selection selA = selector.select("질문A", largePool);
            final FewShotSelector.Selection selB = selector.select("질문Z", largePool);

            assertThat(selA.ids()).isNotEqualTo(selB.ids());
        }

        @Test
        void ids는_정렬된_순서로_반환된다() {
            final List<ZzolBotSessionEntity> largePool = pool(20);

            final FewShotSelector.Selection selection = selector.select("질문", largePool);

            final List<Long> sorted = selection.ids().stream().sorted().toList();
            assertThat(selection.ids()).isEqualTo(sorted);
        }

        @Test
        void ids의_크기와_examples의_크기가_일치한다() {
            final List<ZzolBotSessionEntity> largePool = pool(10);

            final FewShotSelector.Selection selection = selector.select("질문", largePool);

            assertThat(selection.ids()).hasSize(selection.examples().size());
        }
    }
}
