package coffeeshout.user.domain.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.nickname.NameValidator;
import coffeeshout.profanity.domain.ProfanityChecker;
import coffeeshout.user.domain.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class NicknameValidator implements NameValidator {

    private final ProfanityChecker profanityChecker;

    @Override
    public void validate(String name) {
        if (profanityChecker.contains(name)) {
            throw new BusinessException(
                    UserErrorCode.NICKNAME_CONTAINS_PROFANITY,
                    "비속어가 포함된 닉네임입니다. 입력값: '" + name + "'"
            );
        }
    }
}
