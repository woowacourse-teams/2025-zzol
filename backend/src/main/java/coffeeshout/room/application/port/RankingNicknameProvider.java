package coffeeshout.room.application.port;

import java.time.LocalDateTime;
import java.util.Set;

public interface RankingNicknameProvider {

    Set<String> findRankingNicknamesBetween(LocalDateTime start, LocalDateTime end, int limit);
}
