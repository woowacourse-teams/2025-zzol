package coffeeshout.minigame.infra.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class MiniGameResultBulkRepositoryImpl implements MiniGameResultBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void bulkInsert(List<MiniGameResultEntity> resultEntities) {
        if (resultEntities.isEmpty()) {
            return;
        }

        final String sql = """
                INSERT INTO mini_game_result (mini_game_play_id, player_id, player_rank, score, mini_game_type, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                final MiniGameResultEntity entity = resultEntities.get(i);
                ps.setLong(1, entity.getMiniGamePlay().getId());
                ps.setLong(2, entity.getPlayer().getId());
                ps.setInt(3, entity.getRank());
                ps.setLong(4, entity.getScore());
                ps.setString(5, entity.getMiniGameType().name());
                ps.setTimestamp(6, Timestamp.valueOf(entity.getCreatedAt()));
            }

            @Override
            public int getBatchSize() {
                return resultEntities.size();
            }
        });
    }
}
