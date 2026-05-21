package coffeeshout.user.infra.persistence;

import coffeeshout.user.application.port.UserCreationPort;
import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.UserNickname;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserCreateAttemptHelper implements UserCreationPort {

    private final UserJpaRepository userJpaRepository;
    private final OAuthAccountJpaRepository oAuthAccountJpaRepository;

    @Transactional
    public User attempt(UserNickname nickname, OAuthAccount oAuthAccount) {
        final UserCode code = UserCode.generate();
        final UserEntity userEntity = new UserEntity(code.value(), nickname.value());
        final UserEntity savedUser = userJpaRepository.save(userEntity);

        final OAuthAccountEntity oAuthAccountEntity = new OAuthAccountEntity(
                savedUser,
                oAuthAccount.provider().getRegistrationId(),
                oAuthAccount.providerUserId(),
                oAuthAccount.email()
        );
        final OAuthAccountEntity savedOAuth = oAuthAccountJpaRepository.save(oAuthAccountEntity);

        return savedUser.toDomain(savedOAuth);
    }
}
