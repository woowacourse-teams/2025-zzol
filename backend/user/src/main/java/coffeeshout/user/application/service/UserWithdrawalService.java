package coffeeshout.user.application.service;

import coffeeshout.user.application.port.ReportAnonymizationPort;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import coffeeshout.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final ReportAnonymizationPort reportAnonymizationPort;
    private final UserRepository userRepository;

    @Transactional
    public void withdraw(Long userId) {
        reportAnonymizationPort.clearUserCodeByUserId(userId);
        userRepository.softDeleteById(userId);
        TransactionSynchronizationManager.registerSynchronization(deleteRefreshTokenAfterCommit(userId));
    }

    private TransactionSynchronization deleteRefreshTokenAfterCommit(Long userId) {
        return new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    refreshTokenRepository.deleteAllByUserId(userId);
                } catch (Exception e) {
                    log.error("회원 탈퇴 후 리프레시 토큰 삭제 실패 userId={}: {}", userId, e.getMessage(), e);
                }
            }
        };
    }
}
