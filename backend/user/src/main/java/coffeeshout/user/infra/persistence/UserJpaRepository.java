package coffeeshout.user.infra.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUserCode(String userCode);

    List<UserEntity> findAllByNickname(String nickname);

    // @SQLRestriction을 우회하여 soft delete된 사용자를 포함해 조회한다 (테스트 및 어드민 전용)
    @Query(value = "SELECT * FROM app_user WHERE id = :id", nativeQuery = true)
    Optional<UserEntity> findByIdIgnoringDeletedAt(@Param("id") Long id);
}
