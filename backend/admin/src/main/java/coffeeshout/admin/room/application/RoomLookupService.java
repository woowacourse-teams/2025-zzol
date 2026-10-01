package coffeeshout.admin.room.application;

import coffeeshout.admin.room.domain.RoomLookupRepository;
import coffeeshout.admin.room.domain.RoomMiniGameResult;
import coffeeshout.admin.room.domain.RoomPlayer;
import coffeeshout.admin.room.domain.RoomRouletteResult;
import coffeeshout.admin.room.domain.RoomSummary;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 드릴다운.
 *
 * <p>문의가 들어왔을 때("우리 방 결과가 이상해요") 이 화면 하나로 답한다.
 * Grafana 는 집계만 보여주므로 개별 방에서 누가 어떤 점수를 냈고 최종 확률이 얼마였는지는
 * 여기서만 확인할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomLookupService {

    private static final int PAGE_SIZE = 20;

    private final RoomLookupRepository roomLookupRepository;

    public Page<RoomSummary> search(String joinCode, int page) {
        return roomLookupRepository.search(joinCode, PageRequest.of(page, PAGE_SIZE));
    }

    public RoomDetail findDetail(Long roomId) {
        final RoomSummary summary = roomLookupRepository
                .findSummaryById(roomId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_EXIST, "방을 찾을 수 없습니다: " + roomId));

        return new RoomDetail(
                summary,
                roomLookupRepository.findPlayers(roomId),
                roomLookupRepository.findMiniGameResults(roomId),
                roomLookupRepository.findRouletteResult(roomId).orElse(null));
    }

    /**
     * @param roulette 룰렛까지 못 간 방은 null 이다. 중도 이탈이 정상 경로라 예외가 아니다.
     */
    public record RoomDetail(
            RoomSummary summary,
            List<RoomPlayer> players,
            List<RoomMiniGameResult> miniGameResults,
            RoomRouletteResult roulette) {

        public Optional<RoomRouletteResult> rouletteResult() {
            return Optional.ofNullable(roulette);
        }
    }
}
