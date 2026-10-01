package coffeeshout.admin.room.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomLookupRepository {

    /** {@code joinCode}가 비어 있으면 최근 방부터 전체를 돌려준다. */
    Page<RoomSummary> search(String joinCode, Pageable pageable);

    Optional<RoomSummary> findSummaryById(Long roomId);

    List<RoomPlayer> findPlayers(Long roomId);

    List<RoomMiniGameResult> findMiniGameResults(Long roomId);

    Optional<RoomRouletteResult> findRouletteResult(Long roomId);

    /**
     * 기간 안에 만들어진 방을 한 줄씩. 구간 나누기는 서비스가 한다.
     *
     * <p>방 수만큼 행을 읽는다. 기간에 상한이 있어야 하는 이유이고, 그 상한은 요청을 받는
     * 컨트롤러가 건다.
     */
    List<RoomSnapshot> findSnapshots(LocalDateTime from, LocalDateTime to);
}
