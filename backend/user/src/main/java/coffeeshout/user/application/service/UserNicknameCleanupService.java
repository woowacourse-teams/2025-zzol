package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.nickname.NicknamesCollectedEvent;
import coffeeshout.global.nickname.ProfanityChecker;
import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
    private final ProfanityChecker profanityChecker;

    /**
     * 랭킹 수집 회차마다 사전에 걸리는 회원 프로필 닉네임을 생성 닉네임으로 교체한다 (#1777).
     *
     * <p>{@link #onProfanityWordBlocked}만으로는 부족하다. 그쪽은 차단된 단어와 <b>똑같은</b> 닉네임만 찾는데,
     * AI 사후 감사는 닉네임 전체가 아니라 비속어 <b>조각</b>을 사전에 등록한다. "씨발이닷"이 FLAGGED면 "씨발"이
     * 등록되고, 정작 그 회원의 닉네임은 남는다. 여기서는 사전 조회를 {@link ProfanityChecker}에 맡겨
     * 조각 일치와 정규화(leet 치환·특수문자 제거)까지 같은 기준으로 판정한다.
     *
     * <p>차단 시점이 아니라 수집 시점에 도는 이유는 트라이 갱신이 Redis pub/sub이라 비동기이기 때문이다.
     * 단어를 등록한 직후에는 이 인스턴스의 트라이에 그 단어가 아직 없을 수 있다.
     */
    @EventListener
    @Transactional
    public void onNicknamesCollected(NicknamesCollectedEvent event) {
        final List<User> targets = event.nicknames().stream()
                .filter(profanityChecker::contains)
                .map(this::toNickname)
                .flatMap(Optional::stream)
                .flatMap(nickname -> userRepository.findAllByNickname(nickname).stream())
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        log.info("랭킹 검열로 회원 닉네임 일괄 교체: userCount={}", targets.size());
        replaceAll(targets);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onProfanityWordBlocked(ProfanityWordBlockedEvent event) {
        toNickname(event.word())
                .map(userRepository::findAllByNickname)
                .filter(users -> !users.isEmpty())
                .ifPresent(users -> {
                    log.info("비속어 차단으로 닉네임 일괄 교체: word={}, userCount={}", event.word(), users.size());
                    replaceAll(users);
                });
    }

    private void replaceAll(List<User> users) {
        for (final User user : users) {
            final UserNickname newNickname = new UserNickname(nicknameDefaultGenerator.generate());
            user.changeNickname(newNickname);
            userRepository.save(user);
            log.debug("닉네임 교체 완료: userId={}, newNickname={}", user.getId(), newNickname.value());
        }
    }

    /** 회원 닉네임이 될 수 없는 값(공백·길이 초과)은 교체 대상에서 뺀다. 예외를 그대로 올리면 같은 이벤트의 다른 리스너까지 함께 죽는다. */
    private Optional<UserNickname> toNickname(String value) {
        try {
            return Optional.of(new UserNickname(value));
        } catch (BusinessException e) {
            return Optional.empty();
        }
    }
}
