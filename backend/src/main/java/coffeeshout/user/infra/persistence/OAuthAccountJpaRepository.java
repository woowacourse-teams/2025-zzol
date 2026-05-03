package coffeeshout.user.infra.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccountEntity, Long> {

    @Query("SELECT o FROM OAuthAccountEntity o JOIN FETCH o.user WHERE o.provider = :provider AND o.providerUserId = :providerUserId")
    Optional<OAuthAccountEntity> findByProviderAndProviderUserIdWithUser(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId
    );

    Optional<OAuthAccountEntity> findByUser_Id(Long userId);

    List<OAuthAccountEntity> findAllByUser_IdIn(List<Long> userIds);
}
