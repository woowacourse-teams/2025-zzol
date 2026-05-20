package coffeeshout.room.domain.player;

import java.time.LocalDateTime;
import java.util.Set;

public interface RankedNicknameReader {

    Set<String> findRankedNicknames(LocalDateTime start, LocalDateTime end, int limit);
}
