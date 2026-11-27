package coffeeshout.profanity.domain.audit;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * 한 검열 회차에서 CLEAN 판정 일부를 운영자 검토 표본으로 뽑는다. 표본 중 운영자가 차단한 비율로 CLEAN 전체의
 * 미탐률을 추정한다.
 *
 * <p>회차마다 새로 만든다. 상한이 회차 단위라서, 배치마다 새로 만들면 적체를 몰아 처리하는 회차에 상한이 배치
 * 수만큼 곱해진다. 한 회차는 스레드 하나에서 돌므로 동기화하지 않는다.
 */
public class CleanSampler {

    private static final int BUCKETS = 10_000;

    private final long threshold;
    private int remaining;

    /**
     * @param ratio CLEAN 중 뽑을 비율. 해상도는 0.0001이다
     * @param max   이 회차에 뽑을 표본 상한
     */
    public CleanSampler(double ratio, int max) {
        this.threshold = Math.round(ratio * BUCKETS);
        this.remaining = max;
    }

    /**
     * 판정이 끝난 행이 CLEAN이고 뽑히면 표본으로 표시하고 예산을 하나 쓴다.
     *
     * <p>이미 표본인 행은 예산을 쓰지 않는다. 벌크 저장이 실패하면 건별 폴백이 같은 엔티티를 다시 넘기기 때문이다.
     *
     * @return 이번 호출로 새로 표본이 됐는지
     */
    public boolean trySample(NicknameAudit audit) {
        if (remaining <= 0 || audit.getStatus() != NicknameAuditStatus.CLEAN || !isPicked(audit.getNickname())) {
            return false;
        }
        if (!audit.markReviewSample()) {
            return false;
        }
        remaining--;
        return true;
    }

    /**
     * 닉네임 해시로 뽑아 같은 닉네임은 늘 같은 결과가 나온다.
     *
     * <p>{@code String.hashCode}를 쓰지 않는다. 스텁 검열기가 FLAGGED를 그 해시로 고르므로 같은 해시를 쓰면
     * FLAGGED로 빠진 닉네임과 표본 후보가 겹쳐 local에서 표본이 하나도 안 나올 수 있다.
     */
    private boolean isPicked(String nickname) {
        final CRC32 crc = new CRC32();
        crc.update(nickname.getBytes(StandardCharsets.UTF_8));
        return crc.getValue() % BUCKETS < threshold;
    }
}
