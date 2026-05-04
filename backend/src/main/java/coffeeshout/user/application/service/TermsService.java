package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final UserJpaRepository userJpaRepository;

    @Transactional
    public void agreeTerms(Long userId) {
        final UserEntity userEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        userEntity.agreeTerms();
    }
}
