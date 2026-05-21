package coffeeshout.user.infra.persistence;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.UserStats;
import coffeeshout.user.domain.repository.UserStatsRepository;
import coffeeshout.user.domain.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
                    final UserEntity userEntity = findUserEntityById(userStats.getUserId());
                    return new UserStatsEntity(userEntity);
                });
        entity.update(userStats);
        return userStatsJpaRepository.save(entity).toDomain();
    }

    private UserStats createEmpty(Long userId) {
        final UserEntity userEntity = findUserEntityById(userId);
        try {
            return userStatsJpaRepository.save(new UserStatsEntity(userEntity)).toDomain();
        } catch (DataIntegrityViolationException e) {
            return userStatsJpaRepository.findByUser_Id(userId)
                    .map(UserStatsEntity::toDomain)
                    .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
        }
    }

    private UserEntity findUserEntityById(Long userId) {
        return userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
    }
}
