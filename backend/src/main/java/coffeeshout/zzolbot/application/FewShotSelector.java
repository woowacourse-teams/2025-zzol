package coffeeshout.zzolbot.application;

import coffeeshout.zzolbot.domain.FewShotExample;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class FewShotSelector {

    private static final int EXAMPLE_LIMIT = 5;

    public record Selection(List<FewShotExample> examples, List<Long> ids) {}

    public Selection select(String question, List<FewShotExample> pool) {
        if (pool.isEmpty()) {
            return new Selection(List.of(), List.of());
        }

        final List<FewShotExample> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random((long) question.hashCode()));

        final List<FewShotExample> selected = List.copyOf(
                shuffled.subList(0, Math.min(EXAMPLE_LIMIT, shuffled.size()))
        );
        final List<Long> ids = selected.stream()
                .map(FewShotExample::id)
                .sorted()
                .toList();
        return new Selection(selected, ids);
    }
}
