package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.report.domain.ReportAnonymizationRepository;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final ReportAnonymizationRepository reportAnonymizationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void withdraw(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
        reportAnonymizationRepository.clearUserCodeByUserId(userId);
        userRepository.deleteById(userId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshTokenRepository.deleteAllByUserId(userId);
            }
        });
    }
}
