package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNicknameCleanupService {

    private final UserRepository userRepository;
    private final NicknameDefaultGenerator nicknameDefaultGenerator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onProfanityWordBlocked(ProfanityWordBlockedEvent event) {
        final UserNickname blocked;
        try {
            blocked = new UserNickname(event.word());
        } catch (BusinessException e) {
            return;
        }
        final List<User> users = userRepository.findAllByNickname(blocked);

        if (users.isEmpty()) {
            return;
        }

        log.info("비속어 차단으로 닉네임 일괄 교체: word={}, userCount={}", event.word(), users.size());

        for (final User user : users) {
            final UserNickname newNickname = new UserNickname(nicknameDefaultGenerator.generate());
            user.changeNickname(newNickname);
            userRepository.save(user);
            log.debug("닉네임 교체 완료: userId={}, newNickname={}", user.getId(), newNickname.value());
        }
    }
}
