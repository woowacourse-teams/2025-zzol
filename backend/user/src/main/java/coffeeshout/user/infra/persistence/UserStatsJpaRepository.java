package coffeeshout.user.infra.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatsJpaRepository extends JpaRepository<UserStatsEntity, Long> {

    Optional<UserStatsEntity> findByUser_Id(Long userId);
}
