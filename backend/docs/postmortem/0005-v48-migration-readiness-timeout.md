# 0005. V48 마이그레이션이 dev 배포 readiness를 넘겨 반쯤 적용된 채 남음

- 날짜: 2026-09-27
- 심각도: P1
- 상태: 작성중

## 요약

2026-09-27 dev 배포가 V48 마이그레이션에서 두 번 실패했다. V48은 120만 행짜리 `mini_game_result`에 컬럼과 인덱스를 한 ALTER로 추가하고, `player`와 JOIN해 전체 행을 백필했다. 이 작업이 앱 기동 경로에서 돌면서 readiness 150초를 넘겼고, 배포 스크립트가 그린을 내려 롤백했다. 그런데 MySQL 서버에 이미 들어간 ALTER는 취소되지 않고 나중에 끝났다. 그래서 컬럼과 인덱스만 있고 백필과 flyway 기록은 없는 상태가 남았고, 재시도는 `Duplicate column name 'user_id'`로 실패했다. 사용자 영향은 없었다. 블루가 계속 트래픽을 받았고 prod는 V47 그대로였다. 같은 V48이 prod에 올라갔다면 데이터가 더 많아 같은 방식으로 실패했을 것이다. 수정은 #1841에서 했다.

## 타임라인

시각은 KST다.

| 시점 | 사건 |
|------|------|
| 13:54 | #1796 merge. V47 파일명이 겹쳐 CI의 `FlywayMigrationIntegrationTest`가 실패하고 배포까지 가지 않음 |
| 14:14 | #1837 merge. 내 기록 마이그레이션을 V48로 옮김 |
| 14:20:58 | 그린이 기동 중 `Migrating schema coffee_shout to version "48"` 로그를 남김. 그 뒤로 로그가 없음 |
| 14:22:52 | readiness 150회 초과. 그린을 `docker stop --timeout 30`으로 내리고 롤백 |
| 이후, 시각 미확인 | 서버에 남은 ALTER가 끝나 `user_id` 컬럼과 인덱스가 생김. 연결이 끊겨 백필 UPDATE는 실행되지 않음 |
| 16:00:33 | 같은 run 재시도. V48을 처음부터 실행하다 `Duplicate column name 'user_id'`로 실패하고 flyway에 V48 `success=0` 기록이 남음 |
| 16:02 이후 | 컨테이너 재시작마다 `Detected failed migration to version 48`로 기동 실패, 다시 롤백 |
| 복구 | dev DB에서 인덱스와 컬럼을 DROP(55초), V48 실패 기록 삭제로 V47 상태로 되돌림 |

## 임팩트

- dev 배포가 14:20부터 복구까지 막혔다. 그동안 dev는 이전 버전 블루로 서비스했다.
- dev DB 스키마가 flyway 기록과 어긋난 상태로 남았다. 수동 복구가 필요했다.
- prod 영향은 없다. prod는 V47 상태였고 V48은 승격 전이었다.

## 근본 원인

**마이그레이션 시간이 데이터 양에 비례하는데, 그 마이그레이션이 시간 제한이 있는 앱 기동 경로에서 돌았다.**

1. **한 ALTER에 컬럼과 인덱스를 함께 넣었다.** nullable 컬럼 추가는 원래 `INSTANT`로 즉시 끝난다. 같은 문장에 인덱스 추가가 있으면 문장 전체가 `INSTANT`를 못 쓰고, MySQL이 테이블을 통째로 다시 쓰는 방식을 고른다. `ALGORITHM`을 적지 않아 이 선택이 에러 없이 조용히 일어났다. 복구 때 같은 둘을 DROP하는 데 55초가 걸려 재구성 비용을 짐작할 수 있다. 실행 계획을 직접 확인하지는 않았다.
2. **120만 행 백필 UPDATE를 같은 마이그레이션에 넣었다.** 1의 재구성 뒤에 `player` 60만 행과 JOIN하는 UPDATE가 이어진다. 둘을 합치면 150초 안에 끝나기 어렵다.
3. **테스트 데이터로는 드러나지 않았다.** `FlywayMigrationIntegrationTest`는 빈 DB에서 돌아 V48이 즉시 끝난다. 마이그레이션 시간을 재는 검사는 없다.

**반쯤 적용된 상태가 남은 이유.** MySQL DDL은 트랜잭션으로 되돌릴 수 없다. 배포 스크립트의 롤백은 컨테이너와 nginx, `.env`만 되돌리고 DB는 건드리지 않는다. 컨테이너가 죽어도 서버에서 실행 중인 ALTER는 계속 돈다.

### 빗나간 가설

- **"graceful shutdown이 스위치 뒤에 돌아서"**: 배포 스크립트는 nginx 전환 뒤에 블루를 내린다. 이번 실패는 그 전 단계인 readiness에서 일어나 shutdown 순서와 무관했다. 전환 뒤 종료는 블루그린의 정상 순서다.
- **"블루의 트랜잭션이 메타데이터 락을 쥐고 있어서"**: 처음에는 `mini_game_result` 인덱스의 cardinality 8을 보고 테이블이 작다고 판단했다. 작은 테이블의 ALTER가 2분 걸릴 리 없으니 락 대기라고 추정했다. 하지만 cardinality는 행 수가 아니라 서로 다른 값의 수였고, 실제 행 수는 120만이었다. 락 대기가 전혀 없었다고 확정할 수는 없다. 다만 재구성과 백필만으로도 150초를 넘기기에 충분해 주원인에서 뺐다.
- **첫 조회에서 컬럼이 없던 이유**: 복구 전 조회를 prod DB에 해서 "컬럼 없음, V48 기록 없음"을 dev 상태로 오인했다. `SELECT @@hostname, DATABASE()`로 대상을 먼저 확인해야 했다.

## 대응

- dev DB에서 `SHOW PROCESSLIST`로 남은 ALTER가 없는지 확인하고, 인덱스와 컬럼을 DROP한 뒤 V48 실패 기록을 지웠다.
- #1841에서 V48을 고쳤다.
  - 컬럼 추가는 V48에 `ALGORITHM=INSTANT`로, 인덱스 추가는 V48_1에 `ALGORITHM=INPLACE, LOCK=NONE`로 나눴다. 명시한 방식을 쓸 수 없으면 MySQL이 에러를 낸다.
  - 파일 하나에 DDL 하나만 뒀다. 한 파일의 두 번째 DDL이 실패하면 첫 번째만 커밋된 채 남기 때문이다.
  - 앞에 `lock_wait_timeout = 10`을 둬 메타데이터 락을 기본값 1년 동안 기다리지 않게 했다.
  - 백필을 마이그레이션에서 빼고 아래 절차로 옮겼다.

### 배포 후 백필 절차

V48이 적용되고 **nginx 전환 뒤 블루 컨테이너가 내려간 것을 확인한 다음** dev와 prod에서 한 번씩 실행한다. 블루는 `user_id`를 모르는 이전 코드라 전환 전까지 저장한 결과가 NULL로 남기 때문이다. `id` 구간을 1만 행씩 나눠 한 번에 잡는 행 락을 짧게 유지한다. 이미 채운 행은 `user_id IS NULL` 조건으로 건너뛰므로 중간에 멈춰도 다시 돌리면 된다.

```bash
ENV=dev   # 또는 prod
MYSQL="docker exec -i ${ENV}-mysql mysql -uroot -p<비밀번호> coffee_shout -N"

echo "SELECT @@hostname, DATABASE();" | $MYSQL   # 대상 DB 확인
MAX=$(echo "SELECT MAX(id) FROM mini_game_result;" | $MYSQL)

for ((s = 0; s <= MAX; s += 10000)); do
  echo "UPDATE mini_game_result r JOIN player p ON p.id = r.player_id
        SET r.user_id = p.user_id
        WHERE r.id >= $s AND r.id < $s + 10000
          AND r.user_id IS NULL AND p.user_id IS NOT NULL;" | $MYSQL
done
```

완료 확인. 0이 나와야 한다. 0이 아니면 같은 루프를 다시 돌린다. `MAX`를 다시 읽으므로 그 사이 들어온 행도 포함된다.

```sql
SELECT COUNT(*) FROM mini_game_result r JOIN player p ON p.id = r.player_id
WHERE r.user_id IS NULL AND p.user_id IS NOT NULL;
```

## 재발 방지 액션

| 액션 | 상태 |
|------|------|
| V48(INSTANT 컬럼 추가)과 V48_1(INPLACE 인덱스 추가)로 나누고 백필을 분리 (#1841) | ☐ |
| dev에서 수정한 V48의 두 ALTER 소요 시간을 재서 #1841 PR에 기록 | ☐ |
| dev·prod 배포 뒤 위 백필 절차 실행, 완료 확인 쿼리 0건 | ☐ |
| 마이그레이션을 앱 기동에서 떼어 배포 파이프라인의 별도 단계로 옮길지 검토 (후속 이슈) | ☐ |

## 교훈

- **큰 테이블의 ALTER에는 `ALGORITHM`과 `LOCK`을 적는다.** 적지 않으면 MySQL이 느린 방식을 조용히 고른다. 적으면 느린 방식이 필요할 때 에러가 나서 배포 전에 드러난다.
- **백필은 스키마 마이그레이션과 나눈다.** 데이터 양에 비례하는 작업을 시간 제한이 있는 기동 경로에 두지 않는다.
- **MySQL DDL은 롤백되지 않는다.** 배포 롤백이 성공해도 DB는 반쯤 적용됐을 수 있다. 마이그레이션 실패 뒤에는 컬럼·인덱스 상태와 `flyway_schema_history`를 함께 본다.
- **DB를 조회하기 전에 대상부터 확인한다.** `SELECT @@hostname, DATABASE()` 한 줄로 dev와 prod 혼동을 막는다.
