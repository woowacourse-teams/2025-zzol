package coffeeshout.user.infra.persistence;

import coffeeshout.user.domain.UserStats;
import coffeeshout.user.domain.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserStatsRepositoryImpl implements UserStatsRepository {

    private final UserStatsJpaRepository userStatsJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public UserStats findOrCreateByUserId(Long userId) {
        return userStatsJpaRepository.findByUser_Id(userId)
                .map(UserStatsEntity::toDomain)
                .orElseGet(() -> createEmpty(userId));
    }

    @Override
    public UserStats save(UserStats userStats) {
        final UserStatsEntity entity = userStatsJpaRepository.findByUser_Id(userStats.getUserId())
                .orElseGet(() -> {
                    final UserEntity userEntity = userJpaRepository.findById(userStats.getUserId()).orElseThrow();
                    return new UserStatsEntity(userEntity);
                });
        entity.update(userStats);
        return userStatsJpaRepository.save(entity).toDomain();
    }

    private UserStats createEmpty(Long userId) {
        final UserEntity userEntity = userJpaRepository.findById(userId).orElseThrow();
        final UserStatsEntity saved = userStatsJpaRepository.save(new UserStatsEntity(userEntity));
        return saved.toDomain();
    }
}
