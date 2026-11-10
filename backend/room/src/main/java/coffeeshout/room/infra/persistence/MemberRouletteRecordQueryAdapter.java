package coffeeshout.room.infra.persistence;

import coffeeshout.gamecommon.MemberRouletteRecordQuery;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원이 참여한 방의 룰렛 결과로 당첨·연속 생존·참여 판수를 세는 {@link MemberRouletteRecordQuery} 구현체(#1794).
 *
 * <p>참여 판정은 회원 player 행이 있는 room_session의 IN 서브쿼리다. 조인이 아니라서 한 세션에 회원 player 행이
 * 둘이어도 결과 행이 중복되지 않는다. 당첨은 winner의 user_id가 회원인지로 본다.
 */
@Component
@RequiredArgsConstructor
public class MemberRouletteRecordQueryAdapter implements MemberRouletteRecordQuery {

    private static final QRouletteResultEntity ROULETTE_RESULT = QRouletteResultEntity.rouletteResultEntity;
    private static final QPlayerEntity PLAYER = QPlayerEntity.playerEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public RouletteRecord findByUserId(long userId) {
        final List<Long> winnerUserIdsNewestFirst = queryFactory
                .select(ROULETTE_RESULT.winner.userId)
                .from(ROULETTE_RESULT)
                .where(ROULETTE_RESULT.roomSession.id.in(JPAExpressions.select(PLAYER.roomSession.id)
                        .from(PLAYER)
                        .where(PLAYER.userId.eq(userId))))
                .orderBy(ROULETTE_RESULT.createdAt.desc(), ROULETTE_RESULT.id.desc())
                .fetch();

        int winCount = 0;
        int survivalStreak = 0;
        for (Long winnerUserId : winnerUserIdsNewestFirst) {
            if (winnerUserId != null && winnerUserId == userId) {
                winCount++;
            } else if (winCount == 0) {
                survivalStreak++;
            }
        }
        return new RouletteRecord(winCount, survivalStreak, winnerUserIdsNewestFirst.size());
    }
}
