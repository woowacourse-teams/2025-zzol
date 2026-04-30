package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.application.service.player.name.PlayerNameAuditService;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.PlayerNameValidator;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final PlayerNameValidator playerNameValidator;
    private final PlayerNameAuditService playerNameAuditService;

    @Transactional
    public User changeNickname(Long userId, String rawNickname) {
        final UserNickname newNickname = new UserNickname(rawNickname);
        playerNameValidator.validate(new PlayerName(newNickname.value()));

        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 회원입니다. id=" + userId));

        user.changeNickname(newNickname);
        final User updated = userRepository.save(user);

        playerNameAuditService.register(newNickname.value());
        return updated;
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 회원입니다. id=" + userId));
    }
}
