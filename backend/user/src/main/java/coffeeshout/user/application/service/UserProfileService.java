package coffeeshout.user.application.service;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.nickname.NameValidator;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.event.UserNicknameRegisteredEvent;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final NameValidator nameValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User changeNickname(Long userId, String rawNickname) {
        final UserNickname newNickname = new UserNickname(rawNickname);
        nameValidator.validate(newNickname.value());

        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 회원입니다. id=" + userId));

        user.changeNickname(newNickname);
        final User updated = userRepository.save(user);

        eventPublisher.publishEvent(new UserNicknameRegisteredEvent(newNickname.value()));
        return updated;
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 회원입니다. id=" + userId));
    }
}
