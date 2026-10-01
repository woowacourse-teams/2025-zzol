package coffeeshout.admin.quality.ui.response;

import coffeeshout.admin.quality.domain.NicknameAuditQuality;

/**
 * @param falsePositive AI 가 걸렀는데 관리자가 허용. 멀쩡한 닉네임을 막고 있었다는 뜻
 * @param falseNegative AI 가 놓쳤는데 관리자가 차단. 걸러야 할 것을 통과시켰다는 뜻
 * @param overrideRate  관리자가 AI 판정을 뒤집은 비율. 검열 모델을 손볼 시점을 알려주는 신호
 */
public record NicknameAuditQualityResponse(
        long total, long agreed, long falsePositive, long falseNegative, double overrideRate) {

    public static NicknameAuditQualityResponse from(NicknameAuditQuality quality) {
        return new NicknameAuditQualityResponse(
                quality.total(),
                quality.agreed(),
                quality.falsePositive(),
                quality.falseNegative(),
                quality.overrideRate());
    }
}
