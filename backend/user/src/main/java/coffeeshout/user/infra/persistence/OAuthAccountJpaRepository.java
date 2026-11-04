package coffeeshout.user.infra.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccountEntity, Long> {

    @Query(
            "SELECT o FROM OAuthAccountEntity o JOIN FETCH o.user WHERE o.provider = :provider AND o.providerUserId = :providerUserId")
    Optional<OAuthAccountEntity> findByProviderAndProviderUserIdWithUser(
            @Param("provider") String provider, @Param("providerUserId") String providerUserId);

    @Query("SELECT o FROM OAuthAccountEntity o JOIN FETCH o.user WHERE o.emailHash = :emailHash")
    List<OAuthAccountEntity> findByEmailHashWithUser(@Param("emailHash") String emailHash);

    Optional<OAuthAccountEntity> findByUser_Id(Long userId);

    List<OAuthAccountEntity> findAllByUser_IdIn(List<Long> userIds);

    // 탈퇴는 soft delete 뒤에 이 삭제를 부른다. JPQL 의 o.user.id 는 Hibernate 7.4.13 부터 app_user 와 JOIN 해
    // UserEntity 의 @SQLRestriction 이 붙고, 이미 탈퇴한 회원이라 0행이 지워진다(#1898). 그래서 외래키를 직접 쓴다.
    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM oauth_account WHERE user_id = :userId", nativeQuery = true)
    void deleteByUser_Id(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM oauth_account WHERE user_id = :userId", nativeQuery = true)
    long countByUserId(@Param("userId") Long userId);
}
