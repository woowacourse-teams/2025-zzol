package coffeeshout.fixture;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.room.application.port.RoomEntityRepository;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

@Component
public class TestDataHelper {

    private static final double DEFAULT_ROULETTE_WEIGHT = 0.7;

    private final RoomRepository roomRepository;
    private final RoomEntityRepository roomEntityRepository;

    public TestDataHelper(RoomRepository roomRepository, RoomEntityRepository roomEntityRepository) {
        this.roomRepository = roomRepository;
        this.roomEntityRepository = roomEntityRepository;
    }

    /**
     * 미니게임을 프로덕션 시작 경로로 태울 수 있는 방을 만든다 — 첫 번째 gamer가 호스트, 나머지가 게스트이며
     * 전원 준비 완료. 명단을 인자로 받는 이유는 프로덕션({@code MiniGameStartConsumer})이 {@code room.getGamers()}를
     * 이벤트에 싣기 때문이다 — 방 플레이어와 이벤트 gamers의 출처를 하나로 맞춘다.
     *
     * <p>인메모리 도메인 {@code Room}과 영속 {@code RoomEntity}를 둘 다 만든다. 둘 중 하나만 있으면
     * {@code GameStartReadyEvent} 처리 중 room_session id나 플레이어 id가 해석되지 않아 미니게임 엔티티·
     * 결과가 저장되지 않는다. {@code :game} 테스트가 {@code :room} infra를 직접 조립하지 않도록
     * 여기 한 곳에 가둔다(ADR-0034).
     */
    public Room 게임_시작_준비된_방_생성(JoinCode joinCode, List<Gamer> gamers) {
        final Gamer host = gamers.get(0);
        final Room room = new Room(joinCode, new PlayerName(host.getName()), host.getUserId(), DEFAULT_ROULETTE_WEIGHT);
        gamers.stream().skip(1).forEach(gamer -> room.joinGuest(new PlayerName(gamer.getName()), gamer.getUserId()));
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        roomEntityRepository.save(new RoomEntity(joinCode.getValue()));
        return room;
    }

    public Room 방_생성(JoinCode joinCode, PlayerName hostName) {
        Room room = new Room(joinCode, hostName, DEFAULT_ROULETTE_WEIGHT);
        return roomRepository.save(room);
    }

    public Room 진행중인_방_생성(JoinCode joinCode, PlayerName hostName) {
        Room room = new Room(joinCode, hostName, DEFAULT_ROULETTE_WEIGHT);
        ReflectionTestUtils.setField(room, "roomState", RoomState.PLAYING);
        return roomRepository.save(room);
    }

    public Room 방_생성(String joinCode, String hostName) {
        return 방_생성(new JoinCode(joinCode), new PlayerName(hostName));
    }

    public Room 진행중인_방_생성(String joinCode, String hostName) {
        return 진행중인_방_생성(new JoinCode(joinCode), new PlayerName(hostName));
    }
}
