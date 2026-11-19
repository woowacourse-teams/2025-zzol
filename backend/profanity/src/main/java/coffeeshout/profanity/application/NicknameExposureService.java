package coffeeshout.profanity.application;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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

    /**
     * 원문을 보여도 되는 닉네임만 돌려준다.
     *
     * <p>검열 행이 하나 이상 있고 그 행이 모두 {@link NicknameAuditStatus#isRevealable()}이어야 한다. 유니크 제약이 (player_name, status)라
     * 같은 닉네임에 상태가 다른 행이 함께 있을 수 있어서, 하나라도 통과가 아니면 가린다.
     *
     * <p>행이 없는 닉네임도 가린다. 랭킹에 쓰는 닉네임은 저장할 때 검열 큐에 들어가므로, 행이 없다는 건 아직
     * 판정을 받지 않았다는 뜻이다. 조회는 상태를 읽기만 하고 검열 테이블에 쓰지 않는다.
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

        return targets.stream()
                .filter(nickname -> allRevealable(statusesByKey.get(collationKey(nickname))))
                .collect(Collectors.toSet());
    }

    private boolean allRevealable(List<NicknameAuditStatus> statuses) {
        return statuses != null && statuses.stream().allMatch(NicknameAuditStatus::isRevealable);
    }

    /**
     * player_name 콜레이션(utf8mb4_unicode_ci)은 대소문자를 구분하지 않고, PAD SPACE라 끝 공백도 무시한다. IN 쿼리가
     * "abc "로 "ABC" 행을 돌려주므로 짝지을 때도 같은 두 차이를 무시한다. 악센트·전각처럼 Java가 같은 기준으로 못
     * 맞추는 차이는 행 없음으로 떨어져 가려진다. 등록도 콜레이션으로 중복을 보므로 그 닉네임은 자기 행을 따로 갖지 못한다.
     */
    private String collationKey(String nickname) {
        return nickname.toLowerCase(Locale.ROOT).stripTrailing();
    }
}
