package coffeeshout.admin.system.ui.response;

import coffeeshout.admin.system.domain.MigrationRecord;
import java.time.Instant;
import java.util.List;

/**
 * @param managed 이 환경이 Flyway 로 스키마를 관리하는지. 로컬은 {@code ddl-auto} 로 뜨기
 *                때문에 false 다. 목록이 비었을 때 "아직 적용된 게 없다"와 "애초에 안 쓴다"를
 *                화면이 구분해서 설명하려면 이 값이 필요하다.
 * @param current 적용된 최신 버전. 코드의 마이그레이션 파일과 눈으로 맞춰 보는 값이다.
 */
public record MigrationsResponse(boolean managed, String current, List<Item> records) {

    public static MigrationsResponse of(boolean managed, List<MigrationRecord> records) {
        return new MigrationsResponse(
                managed,
                records.stream()
                        .map(MigrationRecord::version)
                        .filter(v -> v != null)
                        .findFirst()
                        .orElse(null),
                records.stream().map(Item::from).toList());
    }

    public record Item(
            String version,
            String description,
            String type,
            Instant installedOn,
            boolean success,
            Integer executionTimeMs) {

        public static Item from(MigrationRecord record) {
            return new Item(
                    record.version(),
                    record.description(),
                    record.type(),
                    record.installedOn(),
                    record.success(),
                    record.executionTimeMs());
        }
    }
}
