package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.report.domain.ReportAnonymizationRepository;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // Redis는 트랜잭션 외부: 삭제 성공 후 DB 실패 시 토큰만 소멸된 채 롤백됨 (재로그인으로 복구 가능, 의도된 순서)
        refreshTokenRepository.deleteAllByUserId(userId);
        reportAnonymizationRepository.clearUserCodeByUserId(userId);
        userRepository.deleteById(userId);
    }
}
