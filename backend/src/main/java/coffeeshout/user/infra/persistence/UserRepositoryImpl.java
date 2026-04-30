package coffeeshout.user.infra.persistence;

import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final OAuthAccountJpaRepository oAuthAccountJpaRepository;

    @Override
    public boolean existsByUserCode(UserCode userCode) {
        return userJpaRepository.existsByUserCode(userCode.value());
    }

    @Override
    public User save(User user) {
        if (user.getId() != null) {
            return updateNickname(user);
        }
        return createNew(user);
    }

    private User createNew(User user) {
        final UserEntity userEntity = new UserEntity(
                user.getUserCode().value(),
                user.getNickname().value()
        );
        final UserEntity savedUser = userJpaRepository.save(userEntity);

        final OAuthAccount oAuthAccount = user.getOAuthAccount();
        final OAuthAccountEntity oAuthAccountEntity = new OAuthAccountEntity(
                savedUser,
                oAuthAccount.provider().getRegistrationId(),
                oAuthAccount.providerUserId(),
                oAuthAccount.email()
        );
        final OAuthAccountEntity savedOAuth = oAuthAccountJpaRepository.save(oAuthAccountEntity);

        return savedUser.toDomain(savedOAuth);
    }

    private User updateNickname(User user) {
        final UserEntity userEntity = userJpaRepository.findById(user.getId())
                .orElseThrow();
        userEntity.updateNickname(user.getNickname().value());
        final UserEntity savedUser = userJpaRepository.save(userEntity);

        return oAuthAccountJpaRepository.findByUser_Id(savedUser.getId())
                .map(savedUser::toDomain)
                .orElseThrow();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .flatMap(userEntity -> oAuthAccountJpaRepository.findByUser_Id(id)
                        .map(userEntity::toDomain));
    }

    @Override
    public Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return oAuthAccountJpaRepository
                .findByProviderAndProviderUserIdWithUser(provider, providerUserId)
                .map(oAuth -> oAuth.getUser().toDomain(oAuth));
    }
}
