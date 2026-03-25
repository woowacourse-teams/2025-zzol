package coffeeshout.room.infra.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface RoomJpaRepository extends Repository<RoomEntity, Long> {

    RoomEntity save(RoomEntity roomEntity);

    Optional<RoomEntity> findFirstByJoinCodeOrderByCreatedAtDesc(String joinCode);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RoomEntity r SET r.roomStatus = 'DONE', r.finishedAt = :now " +
            "WHERE r.joinCode = :joinCode AND r.roomStatus != 'DONE'")
    int updateStatusToDone(@Param("joinCode") String joinCode, @Param("now") LocalDateTime now);
}
