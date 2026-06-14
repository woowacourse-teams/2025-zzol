package coffeeshout.room.infra.persistence;

import coffeeshout.room.application.port.PlayerEntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface PlayerJpaRepository extends Repository<PlayerEntity, Long>, PlayerEntityRepository {

    @Override
    @Query("SELECT p FROM PlayerEntity p JOIN FETCH p.roomSession WHERE p.playerName = :playerName")
    java.util.List<PlayerEntity> findAllByPlayerName(String playerName);
}
