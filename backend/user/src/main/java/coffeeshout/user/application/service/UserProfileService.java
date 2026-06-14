package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.nickname.NicknameSubmittedEvent;
import coffeeshout.global.nickname.ProfanityChecker;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserErrorCode;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final ProfanityChecker profanityChecker;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User changeNickname(Long userId, String rawNickname) {
        final UserNickname newNickname = new UserNickname(rawNickname);
        if (profanityChecker.contains(newNickname.value())) {
            throw new BusinessException(UserErrorCode.NICKNAME_CONTAINS_PROFANITY,
                    "비속어가 포함된 닉네임입니다. 입력값: '" + newNickname.value() + "'");
        }

        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 회원입니다. id=" + userId));

        user.changeNickname(newNickname);
        final User updated = userRepository.save(user);

        eventPublisher.publishEvent(new NicknameSubmittedEvent(newNickname.value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 회원입니다. id=" + userId));
    }
}
