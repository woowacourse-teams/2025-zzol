package coffeeshout.room.infra;

import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.friend.domain.RoomMembership;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomMembershipQueryAdapter implements RoomMembershipQuery {

    private final RoomRepository roomRepository;

    @Override
    public Map<Long, RoomMembership> findByUserIds(Collection<Long> userIds) {
        return roomRepository.findAllByUserIds(userIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toMembership(entry.getValue())));
    }

    private RoomMembership toMembership(Room room) {
        return new RoomMembership(room.getJoinCode().getValue(), room.isJoinable());
    }
}
