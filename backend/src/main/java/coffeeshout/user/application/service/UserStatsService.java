package coffeeshout.user.application.service;

import coffeeshout.user.domain.UserStats;
import coffeeshout.user.domain.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserStatsRepository userStatsRepository;

    @Transactional
    public UserStats getStats(Long userId) {
        return userStatsRepository.findOrCreateByUserId(userId);
    }

    @Transactional
    public UserStats updateStats(Long userId, boolean isWinner) {
        final UserStats stats = userStatsRepository.findOrCreateByUserId(userId);
        if (isWinner) {
            stats.recordWin();
        } else {
            stats.recordSurvival();
        }
        return userStatsRepository.save(stats);
    }
}
