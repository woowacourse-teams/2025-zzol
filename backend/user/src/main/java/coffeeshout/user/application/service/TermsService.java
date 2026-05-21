package coffeeshout.user.application.service;

import coffeeshout.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final UserRepository userRepository;

    @Transactional
    public void agreeTerms(Long userId) {
        userRepository.agreeTerms(userId);
    }
}
