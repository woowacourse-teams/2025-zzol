package coffeeshout.user.infra.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByUserCode(String userCode);

    Optional<UserEntity> findByUserCode(String userCode);
}
