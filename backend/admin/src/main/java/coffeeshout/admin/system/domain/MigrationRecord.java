package coffeeshout.admin.system.domain;

import java.time.Instant;

/**
 * Flyway 가 적용한 마이그레이션 한 줄.
 *
 * <p>이걸 화면에 두는 이유는 <b>배포된 스키마 버전이 코드와 맞는지</b>를 서버에 들어가지
 * 않고 확인하기 위해서다. Boot 4 로 올릴 때 오토컨피그 모듈이 빠져 마이그레이션이 조용히
 * 실행되지 않은 적이 있었고(#1606), 그때는 아무 화면에도 티가 나지 않았다.
 *
 * @param success 실패한 줄도 그대로 보여준다. 실패 기록이 남아 있으면 다음 배포가 막히므로,
 *                그 사실을 화면에서 바로 알아야 한다.
 */
public record MigrationRecord(
        String version,
        String description,
        String type,
        Instant installedOn,
        boolean success,
        Integer executionTimeMs) {}
