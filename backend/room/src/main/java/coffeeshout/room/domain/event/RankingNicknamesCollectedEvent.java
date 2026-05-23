package coffeeshout.room.domain.event;

import java.util.Set;

public record RankingNicknamesCollectedEvent(Set<String> nicknames) {
}
