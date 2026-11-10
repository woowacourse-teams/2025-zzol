package coffeeshout.profanity.application;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 불특정 다수에게 보이는 화면(랭킹)에 닉네임 원문을 내보내도 되는지 정한다(#1906).
 *
 * <p>검열은 주기 배치라 사전을 우회한 닉네임은 판정 전까지 걸러지지 않는다. 그래서 판정이 끝나
 * 통과한 닉네임만 보이고, 나머지는 판정이 없는 것으로 보고 가린다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NicknameExposureService {

    private final NicknameAuditRepository auditRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 원문을 보여도 되는 닉네임만 돌려준다.
     *
     * <p>검열 행이 하나 이상 있고 그 행이 모두 {@link NicknameAuditStatus#isRevealable()}이어야 한다. 유니크 제약이 (player_name, status)라
     * 같은 닉네임에 상태가 다른 행이 함께 있을 수 있어서, 하나라도 통과가 아니면 가린다.
     *
     * <p>행이 없는 닉네임은 검열 큐에 넣으라고 알린다. 등록 경로가 룰렛 당첨과 회원 닉네임 변경뿐이라 미니게임
     * 랭킹에만 오른 닉네임은 행이 없고, 알리지 않으면 판정받을 길이 없어 영영 가려진다.
     */
    public Set<String> findRevealable(Collection<String> nicknames) {
        final Set<String> targets = nicknames.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (targets.isEmpty()) {
            return Set.of();
        }

        final Map<String, List<NicknameAuditStatus>> statusesByKey =
                auditRepository.findAllByNicknameIn(targets).stream()
                        .collect(Collectors.groupingBy(
                                audit -> collationKey(audit.getNickname()),
                                Collectors.mapping(NicknameAudit::getStatus, Collectors.toList())));

        final Set<String> revealable = new HashSet<>();
        final Set<String> unregistered = new HashSet<>();
        for (final String nickname : targets) {
            final List<NicknameAuditStatus> statuses = statusesByKey.get(collationKey(nickname));
            if (statuses == null) {
                unregistered.add(nickname);
                continue;
            }
            if (statuses.stream().allMatch(NicknameAuditStatus::isRevealable)) {
                revealable.add(nickname);
            }
        }

        if (!unregistered.isEmpty()) {
            eventPublisher.publishEvent(new UnregisteredNicknamesExposedEvent(Set.copyOf(unregistered)));
        }
        return revealable;
    }

    /**
     * player_name 콜레이션(utf8mb4_unicode_ci)은 대소문자를 구분하지 않고, PAD SPACE라 끝 공백도 무시한다. IN 쿼리가
     * "abc "로 "ABC" 행을 돌려주므로 짝지을 때도 같은 두 차이를 무시한다. 악센트·전각처럼 Java가 같은 기준으로 못
     * 맞추는 차이는 행 없음으로 떨어져 가려지고, 조회마다 등록 이벤트가 다시 나간다.
     */
    private String collationKey(String nickname) {
        return nickname.toLowerCase(Locale.ROOT).stripTrailing();
    }
}
