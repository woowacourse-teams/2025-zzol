package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.PlayerNameValidator;
import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.domain.service.UserCodeGenerator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserCodeGenerator userCodeGenerator;
    private final PlayerNameValidator playerNameValidator;
    private final NicknameDefaultGenerator nicknameDefaultGenerator;

    @Transactional
    public User registerOrLogin(OAuthProvider provider, String providerUserId, String email, String suggestedNickname) {
        final Optional<User> existing = userRepository.findByProviderAndProviderUserId(
                provider.getRegistrationId(), providerUserId);
        if (existing.isPresent()) {
            log.debug("기존 회원 로그인: provider={}, providerUserId={}", provider, providerUserId);
            return existing.get();
        }

        final UserNickname nickname = resolveNickname(suggestedNickname);
        final User newUser = new User(
                null,
                userCodeGenerator.generate(),
                nickname,
                new OAuthAccount(provider, providerUserId, email)
        );
        final User savedUser = userRepository.save(newUser);
        log.debug("신규 회원 가입: userCode={}, provider={}", savedUser.getUserCode(), provider);
        return savedUser;
    }

    private UserNickname resolveNickname(String suggested) {
        if (suggested == null || suggested.isBlank()) {
            return new UserNickname(nicknameDefaultGenerator.generate());
        }

        final String trimmed = suggested.length() > UserNickname.MAX_LENGTH
                ? suggested.substring(0, UserNickname.MAX_LENGTH)
                : suggested;

        try {
            playerNameValidator.validate(new PlayerName(trimmed));
            return new UserNickname(trimmed);
        } catch (BusinessException e) {
            log.debug("제안된 닉네임이 검증 실패, 자동 생성으로 대체: suggested={}", suggested);
            return new UserNickname(nicknameDefaultGenerator.generate());
        }
    }
}
