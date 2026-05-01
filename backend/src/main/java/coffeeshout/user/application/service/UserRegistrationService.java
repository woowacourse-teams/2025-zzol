package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.PlayerNameValidator;
import coffeeshout.user.config.UserCodeProperties;
import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.infra.persistence.UserCreateAttemptHelper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserCreateAttemptHelper createAttemptHelper;
    private final UserCodeProperties userCodeProperties;
    private final PlayerNameValidator playerNameValidator;
    private final NicknameDefaultGenerator nicknameDefaultGenerator;

    public User registerOrLogin(OAuthProvider provider, String providerUserId, String email, String suggestedNickname) {
        final Optional<User> existing = userRepository.findByProviderAndProviderUserId(
                provider.getRegistrationId(), providerUserId);
        if (existing.isPresent()) {
            log.debug("기존 회원 로그인: provider={}, providerUserId={}", provider, providerUserId);
            return existing.get();
        }

        final UserNickname nickname = resolveNickname(suggestedNickname);
        final OAuthAccount oAuthAccount = new OAuthAccount(provider, providerUserId, email);

        return saveNewUserWithRetry(nickname, oAuthAccount, provider);
    }

    private User saveNewUserWithRetry(UserNickname nickname, OAuthAccount oAuthAccount, OAuthProvider provider) {
        for (int attempt = 0; attempt < userCodeProperties.maxRetry(); attempt++) {
            try {
                final User savedUser = createAttemptHelper.attempt(nickname, oAuthAccount);
                log.debug("신규 회원 가입: userCode={}, provider={}", savedUser.getUserCode(), provider);
                return savedUser;
            } catch (DataIntegrityViolationException e) {
                log.debug("UserCode 중복 발생, 재시도: {}/{}", attempt + 1, userCodeProperties.maxRetry());
            }
        }
        throw new BusinessException(UserErrorCode.USER_CODE_GENERATION_FAILED,
                "사용자 식별 코드 생성에 실패했습니다. 최대 시도 횟수를 초과했습니다.");
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
