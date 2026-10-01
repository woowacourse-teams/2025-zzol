package coffeeshout.admin.system.domain;

import java.util.List;
import java.util.Optional;

public interface MigrationHistoryRepository {

    /**
     * 적용된 마이그레이션을 최신순으로.
     *
     * <p>이력 테이블이 <b>없을 수도 있다.</b> 로컬은 Flyway 를 끄고 ddl-auto 로 스키마를
     * 만든다. 그때 예외를 던지면 화면 전체가 오류로 덮이는데, 정작 그건 고장이 아니라
     * 그 환경의 정상 상태다. 없으면 빈 결과로 돌려주고 화면이 그 사실을 설명한다.
     */
    List<MigrationRecord> findAll(int limit);

    /** 이력 테이블 자체가 있는지. 없으면 화면이 "이 환경은 Flyway 를 쓰지 않는다"고 적는다. */
    boolean exists();

    /** 가장 높은 버전. 코드의 마이그레이션 파일과 맞춰 보는 용도다. */
    Optional<String> currentVersion();
}
