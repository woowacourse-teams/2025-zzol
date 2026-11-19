# 0007. V49 마이그레이션이 콜레이션 충돌로 dev 배포를 막음

- 날짜: 2026-11-11
- 심각도: P1
- 상태: 작성중
- 관련 포스트모템: [0005 V48 마이그레이션이 dev 배포 readiness를 넘겨 반쯤 적용된 채 남음](0005-v48-migration-readiness-timeout.md)

## 요약

2026-11-11 #1908 merge 뒤 dev 배포가 V49 마이그레이션에서 실패해 롤백됐다. V49는 랭킹 닉네임을 검열 큐에 넣는 백필로, `player.player_name`·`app_user.nickname`과 `player_name_audit.player_name`을 비교한다. dev DB에서 앞의 둘은 `utf8mb4_0900_ai_ci`, 뒤의 하나는 `utf8mb4_unicode_ci`라 MySQL이 `ERROR 1267 Illegal mix of collations`로 문장을 거부했다. 마이그레이션 테스트는 모든 테이블이 같은 콜레이션인 컨테이너에서 돌아 이 차이를 잡지 못했다. 사용자 영향은 없었다. 그린이 이전 버전으로 계속 서비스했고 prod는 V49를 받기 전이었다. 수정은 #1917에서 했다.

## 타임라인

시각은 KST다.

| 시점 | 사건 |
|------|------|
| 19:43:52 | #1908 merge. Backend Deploy (CD) 시작 |
| 19:53:08 | 블루가 V49를 실행했고 404ms 만에 실패. `flyway_schema_history`에 V49 `success=0` 기록 |
| 19:54:53 | 블루 컨테이너가 재시작해 다시 기동. `Detected failed migration to version 49`로 validate 실패 |
| 19:55:07 | readiness 150회 초과. 블루를 내리고 롤백 |
| 이후 | dev DB에서 V49 실패 기록을 확인하고 같은 SELECT를 실행해 `ERROR 1267`을 재현 |
| 이후 | #1917 회귀 테스트가 콜레이션을 섞은 컨테이너에서 같은 에러로 실패하는 것을 확인하고 V49를 수정 |

## 임팩트

- dev 배포가 #1908 이후로 막혔다. 그동안 dev는 이전 버전 그린으로 서비스했다.
- 데이터 피해는 없다. V49는 INSERT…SELECT 한 문장이라 실패와 함께 전부 되돌아갔고, 남은 것은 flyway 실패 기록 한 행이다.
- prod 영향은 없다. prod는 V49 승격 전이다. prod도 compose의 서버 콜레이션 설정이 같아 콜레이션이 섞여 있을 가능성이 높지만 직접 확인하지는 않았다.

## 근본 원인

**dev DB에는 콜레이션이 다른 테이블이 섞여 있는데, 테스트 DB는 한 가지 콜레이션만 쓴다.**

1. **테이블마다 콜레이션이 다르다.** `player`(V1)와 `app_user`(V14)는 `CHARSET=utf8mb4`만 지정했고 콜레이션은 서버 기본값을 따랐다. 이 테이블들이 생길 때 서버 기본값은 MySQL 8의 `utf8mb4_0900_ai_ci`였다. 지금 dev compose는 `--collation-server=utf8mb4_unicode_ci`를 주고 있어서, 이후 생긴 `player_name_audit`은 `utf8mb4_unicode_ci`다. 서버 설정이 언제 바뀌었는지는 확인하지 않았다.
2. **V49가 두 콜레이션을 지정 없이 비교했다.** 두 컬럼 모두 강제성이 `IMPLICIT`로 같아 MySQL이 어느 쪽도 고르지 못하고 에러를 낸다. 앱 코드는 바인드 파라미터와 비교해서 이 문제를 겪지 않는다. 컬럼끼리 비교하는 SQL은 V49가 처음이었다.
3. **테스트가 운영 스키마를 재현하지 못했다.** `RankingNicknameAuditBackfillMigrationTest`는 V48까지 올린 빈 컨테이너에 데이터를 심고 V49를 검증했다. 컨테이너는 모든 테이블을 같은 기본 콜레이션으로 만들어 비교가 그대로 통과했다.

### 첫 에러가 로그에 없었다

CI 배포 로그에는 두 번째 기동의 `Detected failed migration to version 49`만 남아 있었다. 배포 스크립트가 readiness 실패 뒤 컨테이너 로그를 `logs --tail=50`으로 끝부분만 출력하기 때문이다. 원인 SQL 에러는 dev DB의 `flyway_schema_history`에서 V49 실패를 확인하고 V49의 SELECT를 직접 실행해 찾았다.

## 대응

- dev DB `flyway_schema_history`에서 V49 `success=0`, 실행 시간 404ms를 확인했다. 실행 시간이 짧아 0005와 같은 시간 초과가 아니라 SQL 에러라고 판단했다.
- `information_schema.COLUMNS`로 세 컬럼의 콜레이션을 확인하고 V49의 SELECT를 실행해 `ERROR 1267`을 재현했다.
- #1917에서 회귀 테스트가 V48 뒤 `player`·`app_user`를 `utf8mb4_0900_ai_ci`로, `player_name_audit`을 `utf8mb4_unicode_ci`로 바꾸게 했다. 수정 전 V49로 같은 에러가 나는 것을 확인했다.
- V49 비교식에 `COLLATE utf8mb4_unicode_ci`를 붙였다. 검열 컬럼 기준으로 맞춰야 앱의 `existsByNickname`처럼 대소문자와 끝 공백을 무시한다. V49는 어느 환경에서도 성공한 적이 없어 파일을 직접 고쳤다.

## 재발 방지 액션

| 액션 | 상태 |
|------|------|
| V49 비교식 콜레이션 명시와 콜레이션을 섞은 회귀 테스트 (#1917) | ☑ |
| dev DB `flyway_schema_history`의 V49 실패 행 삭제 후 재배포, V49 `success=1` 확인 | ☐ |
| prod 승격 전 prod 세 컬럼의 콜레이션 확인 | ☐ |
| 배포 실패 시 첫 기동의 에러 로그도 남기도록 배포 스크립트 개선 검토 | ☐ |

## 교훈

- 컬럼끼리 비교하는 마이그레이션은 양쪽 콜레이션을 먼저 확인한다. 이 DB는 테이블 생성 시점에 따라 콜레이션이 다르다.
- 마이그레이션 테스트 컨테이너는 운영 스키마와 같지 않다. 데이터 양(0005)과 콜레이션(0007) 모두 테스트에서 드러나지 않았다.
- 마이그레이션 실패를 조사할 때는 배포 로그보다 `flyway_schema_history`의 실행 시간과 성공 여부를 먼저 본다. 이번에는 404ms라는 실행 시간만으로 시간 초과 가설을 바로 버릴 수 있었다.
