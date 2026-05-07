package coffeeshout.global.zzolbot.application;

import coffeeshout.global.zzolbot.infra.ZzolBotSessionEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class FewShotSelector {

    private static final int EXAMPLE_LIMIT = 5;

    public record Selection(List<ZzolBotSessionEntity> examples, List<Long> ids) {}

    public Selection select(String question, List<ZzolBotSessionEntity> pool) {
        if (pool.isEmpty()) {
            return new Selection(List.of(), List.of());
        }

        final List<ZzolBotSessionEntity> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random((long) question.hashCode()));

        final List<ZzolBotSessionEntity> selected = List.copyOf(
                shuffled.subList(0, Math.min(EXAMPLE_LIMIT, shuffled.size()))
        );
        final List<Long> ids = selected.stream()
                .map(ZzolBotSessionEntity::getId)
                .sorted()
                .toList();
        return new Selection(selected, ids);
    }
}
