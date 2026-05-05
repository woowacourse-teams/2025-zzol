package coffeeshout.user.infra.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUserCodeAndDeletedAtIsNull(String userCode);

    List<UserEntity> findAllByNicknameAndDeletedAtIsNull(String nickname);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);
}
