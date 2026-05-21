package coffeeshout.user.infra.persistence;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.domain.UserErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final OAuthAccountJpaRepository oAuthAccountJpaRepository;

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
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
        userEntity.updateNickname(user.getNickname().value());
        final UserEntity savedUser = userJpaRepository.save(userEntity);

        return oAuthAccountJpaRepository.findByUser_Id(savedUser.getId())
                .map(savedUser::toDomain)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
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

    @Override
    public List<User> findAllByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return toDomains(userJpaRepository.findAllById(ids));
    }

    @Override
    public Optional<User> findByUserCode(UserCode userCode) {
        return userJpaRepository.findByUserCode(userCode.value())
                .flatMap(userEntity -> oAuthAccountJpaRepository.findByUser_Id(userEntity.getId())
                        .map(userEntity::toDomain));
    }

    @Override
    @Transactional
    public void softDeleteById(Long userId) {
        final UserEntity userEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
        userEntity.anonymize();
        userEntity.softDelete();
        oAuthAccountJpaRepository.deleteByUser_Id(userId);
    }

    @Override
    @Transactional
    public void agreeTerms(Long userId) {
        final UserEntity userEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        userEntity.agreeTerms();
    }

    @Override
    public List<User> findAllByNickname(UserNickname nickname) {
        final List<UserEntity> users = userJpaRepository.findAllByNickname(nickname.value());
        if (users.isEmpty()) {
            return List.of();
        }
        return toDomains(users);
    }

    private List<User> toDomains(List<UserEntity> users) {
        final List<Long> userIds = users.stream().map(UserEntity::getId).toList();
        final Map<Long, OAuthAccountEntity> oauthByUserId = oAuthAccountJpaRepository
                .findAllByUser_IdIn(userIds).stream()
                .collect(Collectors.toMap(o -> o.getUser().getId(), o -> o));
        return users.stream()
                .flatMap(u -> Optional.ofNullable(oauthByUserId.get(u.getId()))
                        .map(u::toDomain)
                        .stream())
                .toList();
    }
}
