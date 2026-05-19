package coffeeshout.user.domain.repository;

import coffeeshout.user.domain.UserStats;

public interface UserStatsRepository {

    UserStats findOrCreateByUserId(Long userId);

    UserStats save(UserStats userStats);
}
