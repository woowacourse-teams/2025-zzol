package coffeeshout.global.zzolbot.infra.sql;

import java.util.List;
import java.util.Map;

public record SqlQueryResult(
        List<Map<String, Object>> rows,
        boolean truncated
) {
    public SqlQueryResult {
        rows = List.copyOf(rows);
    }

    public static SqlQueryResult empty() {
        return new SqlQueryResult(List.of(), false);
    }
}
