package coffeeshout.report.application;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.ratelimit.ReportRateLimitStore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.exception.ReportErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 신고 제출의 사전 검증(IP 유효성, Rate Limit)을 담당하는 퍼사드.
 * <p>
 * {@link ReportService}의 트랜잭션이 시작되기 전에 Redis I/O를 완료하여 DB 커넥션 점유 시간을 최소화한다.
 */
@Component
@RequiredArgsConstructor
public class ReportFacade {

    private final ReportRateLimitStore rateLimitStore;
    private final ReportService reportService;

    public long submit(String ip, ReportCategory category, MiniGameType gameType, String joinCode, String content) {
        if (ip == null || ip.isBlank()) {
            throw new BusinessException(ReportErrorCode.INVALID_CLIENT_IP, ReportErrorCode.INVALID_CLIENT_IP.getMessage());
        }
        if (!rateLimitStore.tryAcquire(ip)) {
            throw new BusinessException(ReportErrorCode.REPORT_RATE_LIMITED, ReportErrorCode.REPORT_RATE_LIMITED.getMessage());
        }
        return reportService.submit(category, gameType, joinCode, content);
    }
}
