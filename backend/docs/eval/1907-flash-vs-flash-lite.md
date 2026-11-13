# flash-lite 와 최신 flash 의 닉네임 검열 판정 비교 (#1907)

- 측정일: 2026-11-11(순서 고정, 두 모델 짝 비교), 2026-11-13(순서 섞음, flash-lite 단독)
- 질문: 무료 한도 때문에 `gemini-3.5-flash-lite` 를 골랐는데([ADR-0038](../adr/0038-nickname-audit-gemini-call-conditions.md)), 그 대가로 판정 품질을 잃었나?

## 결론

**아직 답하지 못했다. 11-11 측정에서는 flash-lite 가 최신 flash 보다 나쁘다는 근거가 없었지만, 그 측정은 쉬운 세트와 고정된 순서에 기댄 결과였다. 조건을 바꾼 11-13 재측정에서는 flash 가 한 번도 응답하지 않아 짝 비교를 하지 못했다.**

- **11-13 에 `gemini-3.8-flash` 는 호출 12회가 모두 실패했다.** 504 타임아웃 2회, 503 과부하 10회다. 그래서 하드셋과 순서를 섞은 579건의 두 모델 비교는 비어 있다. flash-lite 만 두 세트를 쟀다.
- **하드 골든셋 350건에서 flash-lite 는 천장에 닿지 않는다.** 욕설 재현율 75.0%(27/36), 우회 재현율 89.6%(120/134), 하드 네거티브 오탐 4.7%(5/106)다. 579건에서 100% 였던 재현율이 내려왔으므로 이 세트로는 모델 차이를 볼 수 있다.
- **하드셋의 약한 축은 한영오타와 혐오 계열이다.** 한영오타 55.6%(10/18), 혐오차별 65.0%(13/20), 집단비하 60.0%(3/5)다. 자모분리, 발음변형, 숫자섞기, 닮은꼴문자, 초성숫자혼합, 복합삽입 여섯 축은 94% 이상이다.
- **579건을 다시 재니 flash-lite 의 정상어 오탐이 0/301 에서 7/301(2.3%)로, 반복 간 뒤집힘이 1.2%(7/579)에서 4.4%(20/459)로 늘었다.** 11-13 은 회차마다 닉네임 순서를 섞었다. 순서 때문이라고 단정하지는 못한다. 날짜가 다르고, 503 실패로 표본마다 판정 수가 다르고, 같은 조건을 되풀이한 측정이 아니다.
- **PENDING 이 거의 안 나오는 까닭은 모델이 flagged=true 를 낼 때 confidence 를 높게 주기 때문이다.** 하드셋에서 flagged=true 298행 중 confidence 가 0.85 아래인 행은 10건(3.4%)이다. 틀린 flagged=true 62행 중에는 0건이다. 임계값 0.85 는 틀린 차단을 사람 확인으로 돌리지 못한다(#1911).
- **비열등 한계 2%p 는 잠정값이다. 사용자 확인이 필요하다.** 11-11 결과에만 적용했고 운영 근거가 있는 숫자가 아니다.

flash-lite 선택을 뒤집을 근거는 없다. 지킬 근거도 11-11 의 쉬운 세트 결과뿐이다. flash 가 응답하는 날 하드셋과 순서를 섞은 579건으로 짝 비교를 다시 해야 한다.

## 측정 조건

| 항목 | 11-11 | 11-13 |
| --- | --- | --- |
| 세트 | `golden.csv` 579건 | `golden.csv` 579건, `golden-hard.csv` 350건 |
| 모델 | `gemini-3.8-flash`, `gemini-3.5-flash-lite` | 같음. flash 는 모든 호출이 실패했다 |
| 배치와 반복 | 배치 250건, 3회 반복 | 같음 |
| 닉네임 순서 | 골든셋 파일 순서 그대로 | 회차마다 섞음. 시드 1907 에 회차 번호를 더한다 |
| 타임아웃 | flash 120초와 300초, flash-lite 120초 | flash 300초, flash-lite 120초 |
| 응답 시간 | 실패한 호출도 평균에 넣었다 | 성공한 호출만 센다 |
| 행 단위 기록 | 판정, 사유 | 판정, 사유, flagged, confidence, 배치 안 위치 |
| 호출 단위 기록 | 없음 | 성공 여부, 걸린 시간, 토큰 수, 실패 원인 |

두 날 모두 운영 프롬프트를 피드백 예시 없이 썼고, SDK 재시도를 꺼서(`NICKNAME_AUDIT_HTTP_ATTEMPTS=1`) 배치 하나가 HTTP 요청 하나다. 호출 사이는 5초 띄웠다. 러너는 `NicknameAuditGoldenTest`(`:profanity:goldenTest`)다.

### 호출 수

`generateContent` 호출은 11-11 에 27회, 11-13 에 27회다. 429 응답은 두 날 모두 없었다.

| 날짜 | 세트 | 모델 | 호출 | 성공 | 실패 |
| --- | --- | --- | --- | --- | --- |
| 11-11 | 579건 | `gemini-3.8-flash` (120초) | 9 | 5 | 4 (타임아웃) |
| 11-11 | 579건 | `gemini-3.5-flash-lite` | 9 | 8 | 1 (503) |
| 11-11 | 579건 | `gemini-3.8-flash` (300초) | 9 | 9 | 0 |
| 11-13 | 하드셋 | `gemini-3.8-flash` 1차 | 6 | 0 | 6 (504 2회, 503 4회) |
| 11-13 | 하드셋 | `gemini-3.5-flash-lite` | 6 | 4 | 2 (503) |
| 11-13 | 579건 | `gemini-3.5-flash-lite` | 9 | 7 | 2 (503) |
| 11-13 | 하드셋 | `gemini-3.8-flash` 2차 | 6 | 0 | 6 (503) |
| 11-13 | 579건 | `gemini-3.8-flash` | 0 | 0 | 하드셋이 전부 실패해 시작하지 않았다 |

### 11-13 flash 실패 12회

시각은 한국 시간이고 실패 응답을 받은 때다. 503 의 메시지는 모두 "This model is currently experiencing high demand" 였다.

| 시도 | 회차-배치 | 건수 | 상태 | 걸린 시간 | 실패 시각 |
| --- | --- | --- | --- | --- | --- |
| 1차 | 1-1 | 250 | 504 DEADLINE_EXCEEDED | 300.3초 | 20:04:19 |
| 1차 | 1-2 | 100 | 504 DEADLINE_EXCEEDED | 298.6초 | 20:09:18 |
| 1차 | 2-1 | 250 | 503 UNAVAILABLE | 4.1초 | 20:09:22 |
| 1차 | 2-2 | 100 | 503 UNAVAILABLE | 85.3초 | 20:10:47 |
| 1차 | 3-1 | 250 | 503 UNAVAILABLE | 6.3초 | 20:10:53 |
| 1차 | 3-2 | 100 | 503 UNAVAILABLE | 1.3초 | 20:10:54 |
| 2차 | 1-1 | 250 | 503 UNAVAILABLE | 2.8초 | 20:14:54 |
| 2차 | 1-2 | 100 | 503 UNAVAILABLE | 55.4초 | 20:15:49 |
| 2차 | 2-1 | 250 | 503 UNAVAILABLE | 50.6초 | 20:16:40 |
| 2차 | 2-2 | 100 | 503 UNAVAILABLE | 11.8초 | 20:16:52 |
| 2차 | 3-1 | 250 | 503 UNAVAILABLE | 89.7초 | 20:18:21 |
| 2차 | 3-2 | 100 | 503 UNAVAILABLE | 25.9초 | 20:18:47 |

### 11-13 flash-lite 실패 4회와 표본별 판정 수

| 세트 | 회차-배치 | 건수 | 상태 | 실패 시각 |
| --- | --- | --- | --- | --- |
| 하드셋 | 1-1 | 250 | 503 UNAVAILABLE | 20:11:17 |
| 하드셋 | 3-1 | 250 | 503 UNAVAILABLE | 20:12:19 |
| 579건 | 1-2 | 250 | 503 UNAVAILABLE | 20:13:00 |
| 579건 | 2-1 | 250 | 503 UNAVAILABLE | 20:13:11 |

회차마다 순서를 섞었으므로 실패한 배치에 실린 닉네임이 회차마다 다르다. 그래서 표본마다 받은 판정 수가 다르다.

| 세트 | 판정 3회 | 판정 2회 | 판정 1회 | 판정 0회 |
| --- | --- | --- | --- | --- |
| 하드셋 350건 | 29 | 142 | 179 | 0 |
| 579건 | 198 | 261 | 120 | 0 |

하드셋은 절반이 넘는 179건(51.1%)이 판정 한 번으로 정해졌다. 판정이 2회인 표본은 둘이 갈리면 앞선 회차 판정을 다수결로 썼다. 아래 11-13 표의 "다수결"은 이 한계를 안고 읽어야 한다. 실패한 호출은 다시 부르지 않았다. flash-lite 는 운영과 한도를 같이 쓴다.

### 지표 정의

- **다수결**: 닉네임마다 판정을 받은 회차의 최빈값이다. 동률이면 앞선 회차 판정을 쓴다.
- **재현율**: 욕설(PROFANE)과 우회(EVASION)를 FLAGGED 나 PENDING 으로 걸러낸 비율이다.
- **정답**: 욕설과 우회는 FLAGGED 나 PENDING, 경계는 PENDING, 정상은 CLEAN 이면 맞은 것으로 센다.
- **뒤집힘**: 판정을 2회 이상 받은 닉네임 중 회차마다 판정이 달랐던 비율이다.
- **3의 법칙**: n 건에서 0건이 나왔을 때 실제 비율의 95% 신뢰 상한을 3/n 으로 잡는다.
- **비열등 한계**: 정답률 차이 -2%p. **잠정값이고 사용자 확인이 필요하다.**
- **confidence**: 모델이 낸 값을 소수 둘째 자리로 반올림한 값이다. flagged=true 이고 confidence 가 0.85 이상이면 FLAGGED, 아래면 PENDING 이 된다.

## 11-13 하드 골든셋: flash-lite 단독

### 지표 (다수결 기준)

| 지표 | 값 |
| --- | --- |
| 욕설(PROFANE) 재현율 | 75.0% (27/36) |
| 우회(EVASION) 재현율 | 89.6% (120/134) |
| 욕설·우회 미탐 | 13.5% (23/170) |
| 정상(CLEAN) 오탐률 | 4.7% (5/106) |
| 경계(AMBIGUOUS) PENDING 비율 | 4.1% (3/74) |
| 경계 판정 분포 CLEAN / FLAGGED / PENDING | 37 / 34 / 3 |
| 회차별 MISSING | 250 / 0 / 250 |
| 성공한 호출 응답 시간 평균 / 최대 | 14.7s / 25.1s (4회) |
| 실패한 호출 | 33.3% (2/6) |
| 호출당 토큰 평균 (입력 / 출력 / 캐시 / 생각) | 1186 / 6417 / 0 / 0 |
| 반복 간 판정 뒤집힘 | 17.0% (29/171) |

### 표본별 판정 수

| 판정을 받은 회차 수 | 표본 수 |
| --- | --- |
| 3 | 29 |
| 2 | 142 |
| 1 | 179 |

### 카테고리별

걸러냄은 FLAGGED 와 PENDING 을 합친 비율이다. 욕설·우회 행에서는 재현율이고 정상 행에서는 오탐률이다.

| 카테고리 | expected | 걸러냄 | CLEAN / FLAGGED / PENDING |
| --- | --- | --- | --- |
| 하드네거티브 | CLEAN | 4.7% (5/106) | 101 / 5 / 0 |
| 복합삽입 | EVASION | 100.0% (18/18) | 0 / 18 / 0 |
| 숫자섞기 | EVASION | 100.0% (18/18) | 0 / 18 / 0 |
| 성적표현 | PROFANE | 93.3% (14/15) | 1 / 14 / 0 |
| 집단비하 | AMBIGUOUS | 60.9% (14/23) | 9 / 13 / 1 |
| 애매 | AMBIGUOUS | 29.4% (10/34) | 24 / 9 / 1 |
| 닮은꼴문자 | EVASION | 100.0% (18/18) | 0 / 18 / 0 |
| 성적표현 | AMBIGUOUS | 71.4% (5/7) | 2 / 5 / 0 |
| 초성숫자혼합 | EVASION | 100.0% (18/18) | 0 / 18 / 0 |
| 집단비하 | PROFANE | 50.0% (2/4) | 2 / 2 / 0 |
| 자모분리 | EVASION | 94.1% (16/17) | 1 / 16 / 0 |
| 한영오타 | EVASION | 55.6% (10/18) | 8 / 10 / 0 |
| 혐오차별 | PROFANE | 62.5% (10/16) | 6 / 10 / 0 |
| 혐오차별 | EVASION | 75.0% (3/4) | 1 / 3 / 0 |
| 발음변형 | EVASION | 94.1% (16/17) | 1 / 16 / 0 |
| 성적표현 | EVASION | 40.0% (2/5) | 3 / 2 / 0 |
| 혐오차별 | AMBIGUOUS | 77.8% (7/9) | 2 / 6 / 1 |
| 집단비하 | EVASION | 100.0% (1/1) | 0 / 1 / 0 |
| 발음변형 | PROFANE | 100.0% (1/1) | 0 / 1 / 0 |
| 자모분리 | AMBIGUOUS | 100.0% (1/1) | 0 / 1 / 0 |

### 배치 안 위치별 오답률

경계(AMBIGUOUS)를 뺀 모든 회차의 행을 센다.

| 배치 안 위치 | 오답 |
| --- | --- |
| 1~50 | 9.5% (15/158) |
| 51~100 | 11.9% (19/159) |
| 101~150 | 8.1% (3/37) |
| 151~200 | 5.3% (2/38) |
| 201~250 | 2.4% (1/41) |

### confidence 분포

판정을 받은 모든 회차의 행을 센다. confidence 는 소수 둘째 자리로 반올림한 값이다.

| confidence 구간 | flagged=true | flagged=false |
| --- | --- | --- |
| 0.50 이상 0.55 미만 | 0 | 1 |
| 0.60 이상 0.65 미만 | 0 | 1 |
| 0.70 이상 0.75 미만 | 2 | 5 |
| 0.75 이상 0.80 미만 | 1 | 1 |
| 0.80 이상 0.85 미만 | 7 | 17 |
| 0.85 이상 0.90 미만 | 33 | 25 |
| 0.90 이상 0.95 미만 | 32 | 50 |
| 0.95 이상 1.00 | 223 | 152 |

| 묶음 | 행 수 | flagged=true | confidence 평균 | 0.85 미만 | flagged=true 중 0.85 미만 |
| --- | --- | --- | --- | --- | --- |
| 전체 | 550 | 298 | 0.939 | 6.4% (35/550) | 3.4% (10/298) |
| PROFANE | 53 | 42 | 0.921 | 13.2% (7/53) | 2.4% (1/42) |
| EVASION | 207 | 185 | 0.959 | 2.9% (6/207) | 0.0% (0/185), 상한 1.6% |
| AMBIGUOUS | 117 | 64 | 0.869 | 17.9% (21/117) | 14.1% (9/64) |
| CLEAN | 173 | 7 | 0.967 | 0.6% (1/173) | 0.0% (0/7), 상한 42.9% |
| 정답 맞음 | 402 | 236 | 0.963 | 2.7% (11/402) | 4.2% (10/236) |
| 정답 틀림 | 148 | 62 | 0.872 | 16.2% (24/148) | 0.0% (0/62), 상한 4.8% |

축별로 묶으면 아래와 같다. 재현율은 그 축의 욕설과 우회 표본을 합친 값이다.

| 축 | 욕설·우회 재현율 | 경계 표본 처리 (CLEAN / FLAGGED / PENDING) |
| --- | --- | --- |
| 자모분리 | 94.1% (16/17) | 0 / 1 / 0 |
| 한영오타 | 55.6% (10/18) | 표본 없음 |
| 발음변형 | 94.4% (17/18) | 표본 없음 |
| 숫자섞기 | 100.0% (18/18), 미탐 상한 16.7% | 표본 없음 |
| 닮은꼴문자 | 100.0% (18/18), 미탐 상한 16.7% | 표본 없음 |
| 초성숫자혼합 | 100.0% (18/18), 미탐 상한 16.7% | 표본 없음 |
| 복합삽입 | 100.0% (18/18), 미탐 상한 16.7% | 표본 없음 |
| 성적표현 | 80.0% (16/20) | 2 / 5 / 0 |
| 혐오차별 | 65.0% (13/20) | 2 / 6 / 1 |
| 집단비하 | 60.0% (3/5) | 9 / 13 / 1 |
| 애매 | 표본 없음 | 24 / 9 / 1 |
| 하드네거티브 | 오탐 4.7% (5/106) | 표본 없음 |

- **하드 네거티브 106건 중 5건을 FLAGGED 로 차단했다.** `콩나물대가리`, `자지러지는중`, `이건니거야`, `꽃병신상`, `짱깨끗해`다. 앞의 3건은 판정이 한 번뿐이다.
- **우회 7축 중 한영오타만 크게 낮다.** 한글 자판 그대로 영문으로 친 욕설 18건 중 8건을 CLEAN 으로 넘겼다.
- **혐오차별과 집단비하는 재현율이 60%대다.** 다만 이 두 축은 라벨 자체의 신뢰도가 낮다([한계](#한계)).
- **애매 34건은 24건을 CLEAN, 9건을 FLAGGED 로 처리했고 PENDING 은 1건이다.** 경계 표본 74건 전체로는 PENDING 이 3건(4.1%)이다.
- **반복 간 뒤집힘은 17.0%(29/171)다.** 579건의 4.4% 보다 4배 가깝게 높다. 어려운 표본일수록 같은 닉네임의 판정이 회차마다 갈린다.

## 11-13 골든셋 579건: 순서를 섞은 flash-lite

### 지표 (다수결 기준)

| 지표 | 값 |
| --- | --- |
| 욕설(PROFANE) 재현율 | 98.7% (78/79) |
| 우회(EVASION) 재현율 | 100.0% (156/156) |
| 욕설·우회 미탐 | 0.4% (1/235) |
| 정상(CLEAN) 오탐률 | 2.3% (7/301) |
| 경계(AMBIGUOUS) PENDING 비율 | 4.7% (2/43) |
| 경계 판정 분포 CLEAN / FLAGGED / PENDING | 16 / 25 / 2 |
| 회차별 MISSING | 250 / 250 / 1 |
| 성공한 호출 응답 시간 평균 / 최대 | 15.8s / 21.9s (7회) |
| 실패한 호출 | 22.2% (2/9) |
| 호출당 토큰 평균 (입력 / 출력 / 캐시 / 생각) | 1298 / 7758 / 0 / 0 |
| 반복 간 판정 뒤집힘 | 4.4% (20/459) |

### 표본별 판정 수

| 판정을 받은 회차 수 | 표본 수 |
| --- | --- |
| 3 | 198 |
| 2 | 261 |
| 1 | 120 |

### 카테고리별

걸러냄은 FLAGGED 와 PENDING 을 합친 비율이다. 욕설·우회 행에서는 재현율이고 정상 행에서는 오탐률이다.

| 카테고리 | expected | 걸러냄 | CLEAN / FLAGGED / PENDING |
| --- | --- | --- | --- |
| 초성 | EVASION | 100.0% (15/15) | 0 / 15 / 0 |
| 정상초성 | CLEAN | 0.0% (0/16), 상한 18.8% | 16 / 0 / 0 |
| 경계걸침정상어 | CLEAN | 14.3% (6/42) | 36 / 6 / 0 |
| 자모분리 | EVASION | 100.0% (15/15) | 0 / 15 / 0 |
| 경계 | AMBIGUOUS | 62.8% (27/43) | 16 / 25 / 2 |
| 일반닉네임 | CLEAN | 0.0% (0/170), 상한 1.8% | 170 / 0 / 0 |
| 유사문자 | EVASION | 100.0% (12/12) | 0 / 12 / 0 |
| 오탐유발정상어 | CLEAN | 1.4% (1/73) | 72 / 1 / 0 |
| 유사발음 | EVASION | 100.0% (20/20) | 0 / 20 / 0 |
| 변형표기 | EVASION | 100.0% (20/20) | 0 / 20 / 0 |
| 이름+욕설 | PROFANE | 95.0% (19/20) | 1 / 19 / 0 |
| 특수문자삽입 | EVASION | 100.0% (15/15) | 0 / 15 / 0 |
| 직접욕설 | PROFANE | 100.0% (30/30) | 0 / 30 / 0 |
| 영타 | EVASION | 100.0% (9/9) | 0 / 9 / 0 |
| 혐오비하 | PROFANE | 100.0% (15/15) | 0 / 15 / 0 |
| 늘이기 | EVASION | 100.0% (10/10) | 0 / 10 / 0 |
| 공백삽입 | EVASION | 100.0% (6/6) | 0 / 6 / 0 |
| 성적표현 | PROFANE | 100.0% (9/9) | 0 / 9 / 0 |
| 복합우회 | EVASION | 100.0% (15/15) | 0 / 15 / 0 |
| 영어욕설 | PROFANE | 100.0% (5/5) | 0 / 5 / 0 |
| 영어변형 | EVASION | 100.0% (9/9) | 0 / 9 / 0 |
| 로마자 | EVASION | 100.0% (10/10) | 0 / 10 / 0 |

### 배치 안 위치별 오답률

경계(AMBIGUOUS)를 뺀 모든 회차의 행을 센다.

| 배치 안 위치 | 오답 |
| --- | --- |
| 1~50 | 2.5% (8/319) |
| 51~100 | 1.5% (4/261) |
| 101~150 | 0.5% (1/188) |
| 151~200 | 1.6% (3/187) |
| 201~250 | 2.2% (4/186) |

### confidence 분포

판정을 받은 모든 회차의 행을 센다. confidence 는 소수 둘째 자리로 반올림한 값이다.

| confidence 구간 | flagged=true | flagged=false |
| --- | --- | --- |
| 0.60 이상 0.65 미만 | 0 | 2 |
| 0.70 이상 0.75 미만 | 3 | 3 |
| 0.80 이상 0.85 미만 | 3 | 10 |
| 0.85 이상 0.90 미만 | 9 | 19 |
| 0.90 이상 0.95 미만 | 34 | 60 |
| 0.95 이상 1.00 | 495 | 598 |

| 묶음 | 행 수 | flagged=true | confidence 평균 | 0.85 미만 | flagged=true 중 0.85 미만 |
| --- | --- | --- | --- | --- | --- |
| 전체 | 1236 | 544 | 0.970 | 1.7% (21/1236) | 1.1% (6/544) |
| PROFANE | 165 | 163 | 0.977 | 0.6% (1/165) | 0.6% (1/163) |
| EVASION | 316 | 314 | 0.978 | 0.3% (1/316) | 0.0% (0/314), 상한 1.0% |
| AMBIGUOUS | 95 | 51 | 0.902 | 9.5% (9/95) | 5.9% (3/51) |
| CLEAN | 660 | 16 | 0.975 | 1.5% (10/660) | 12.5% (2/16) |
| 정답 맞음 | 1124 | 480 | 0.977 | 1.1% (12/1124) | 0.8% (4/480) |
| 정답 틀림 | 112 | 64 | 0.906 | 8.0% (9/112) | 3.1% (2/64) |

### 11-11(순서 고정)과 11-13(순서 섞음)의 차이

같은 모델, 같은 579건, 같은 배치 크기다. 다른 것은 닉네임 순서, 날짜, 표본별 판정 수다.

| 지표 | 11-11 순서 고정 | 11-13 순서 섞음 |
| --- | --- | --- |
| 욕설 재현율 | 100.0% (79/79) | 98.7% (78/79) |
| 우회 재현율 | 100.0% (156/156) | 100.0% (156/156) |
| 정상어 오탐 | 0.0% (0/301), 상한 1.0% | 2.3% (7/301) |
| 경계 판정 분포 CLEAN / FLAGGED / PENDING | 29 / 14 / 0 | 16 / 25 / 2 |
| 반복 간 뒤집힘 | 1.2% (7/579) | 4.4% (20/459) |
| 회차 쌍 판정 불일치율 | 1.19% | 3.98% |

- 두 날의 다수결 판정은 579건 중 556건(96.0%)이 같고 Cohen's κ 는 0.920 이다. 갈린 23건 중 21건은 11-13 쪽이 더 많이 차단한 방향이다.
- 정답 맞음/틀림을 짝지은 McNemar 정확 검정은 전체 b=8, c=2, p=0.109 이고, 정상 301건만 보면 b=7, c=0, p=0.016 이다. b 는 11-11 에만 맞은 건수다.
- 11-11 회차와 11-13 회차 사이의 판정 불일치율은 3.88% 로, 11-13 회차끼리의 3.98% 와 같은 수준이다. 11-13 의 판정은 11-11 과 다른 만큼 자기들끼리도 다르다.
- 새로 생긴 오탐 7건은 `시발택시`, `수박씨발라`, `상병신고식`, `강아지새끼`, `꽃씨발아`, `시발역`, `일병신고`다. 6건이 경계걸침정상어 카테고리다. 회차별 판정과 배치 안 위치는 [부록](#부록-11-13-579건에서-다수결이-틀렸거나-판정이-갈린-표본)에 있다.

**이 차이를 순서 효과라고 단정하지 않는다.** 골든셋 파일은 카테고리별로 묶여 있어서 순서를 고정하면 비슷한 닉네임이 한 배치에 이어서 들어간다. 섞으면 욕설과 정상어가 뒤섞인다. 이게 판정을 바꿨을 수 있다. 하지만 두 측정은 이틀 간격이고, 시드도 하나뿐이고, 11-13 은 503 때문에 판정 3회를 다 받은 표본이 198건뿐이다. 같은 날 같은 조건에서 순서만 바꿔 되풀이해야 순서 효과를 가를 수 있다.

배치 안 위치와 오답률 사이에는 뚜렷한 경향이 없다. 위 "배치 안 위치별 오답률" 표에서 579건은 구간별 0.5%~2.5% 로 오르내린다.

## confidence 분포 요약 (11-13 flash-lite)

세트별 히스토그램은 위 각 절의 "confidence 분포" 표에 있다.

- **flagged=true 일 때 confidence 는 거의 0.85 이상이다.** 0.85 아래는 하드셋 3.4%(10/298), 579건 1.1%(6/544)다. PENDING 은 이 행들에서만 나온다. 11-11 에 PENDING 이 0건이었던 것과 같은 방향이다.
- **경계 표본에서 조금 더 낮다.** flagged=true 인 경계 행 중 0.85 아래는 하드셋 14.1%(9/64), 579건 5.9%(3/51)다. 그래도 경계 표본을 차단할 때 열에 여덟아홉은 0.85 이상을 준다.
- **틀린 판정의 confidence 평균이 맞은 판정보다 낮다.** 하드셋 0.872 대 0.963, 579건 0.906 대 0.977 이다.
- **하지만 틀리게 차단한 행은 임계값으로 걸러지지 않는다.** 하드셋에서 정답이 틀린 flagged=true 62행 중 0.85 아래는 0건(상한 4.8%)이다. 낮은 confidence 는 주로 flagged=false 쪽에 있다. 하드셋의 0.85 아래 35행 중 25행이 flagged=false 다.
- **임계값을 0.90 이나 0.95 로 올리면 PENDING 이 늘어난다.** 하드셋 flagged=true 298행 중 0.90 아래가 43행(14.4%), 0.95 아래가 75행(25.2%)이다. 이 행들이 실제로 사람 확인이 필요한 표본인지는 이 측정으로 알 수 없다.

## 11-11 골든셋 579건: 순서 고정, 두 모델 짝 비교

아래는 11-11 측정 결과다. 순서를 섞지 않았고 응답 시간에 실패한 호출이 섞이지 않게 다시 계산했다.

- 욕설과 우회 재현율은 두 모델 모두 235/235 다. 미탐 0건의 95% 신뢰 상한은 1.3%(3/235)다.
- 정상어 오탐은 flash-lite 0건(0/301, 상한 1.0%), flash 1건(`아저씨발냄새`)이다.
- McNemar 정확 검정은 b=0, c=1, p=1.000 이고 정답률 차이는 +0.17%p(95% 구간 -0.17 ~ +0.51)다. 잠정 비열등 한계 -2%p 안이다.
- 다수결이 엇갈린 9건 중 8건이 경계 표본이다. flash 는 경계 43건 중 22건을, flash-lite 는 14건을 FLAGGED 로 차단했다.
- flash 는 타임아웃 120초에서 호출 9회 중 4회가 실패했다. 300초로 늘린 측정에서 성공한 호출은 평균 72.4초, 최대 200.6초였다. flash-lite 는 평균 19.3초, 최대 28.3초다.

11-11 에 flash 를 고른 근거: `models.list` 가 돌려준 flash 계열 중 lite, 이미지, TTS, Omni 를 뺀 텍스트 모델은 `gemini-2.5-flash`, `gemini-3-flash-preview`, `gemini-3.5-flash`, `gemini-3.6-flash`, `gemini-3.7-flash`, `gemini-3.8-flash` 였고 버전이 가장 높은 것을 골랐다.

### 모델별 지표 (3회 다수결 기준)

| 지표 | gemini-3.8-flash | gemini-3.5-flash-lite |
| --- | --- | --- |
| 욕설(PROFANE) 재현율 | 100.0% (79/79) | 100.0% (79/79) |
| 우회(EVASION) 재현율 | 100.0% (156/156) | 100.0% (156/156) |
| 욕설·우회 미탐 | 0.0% (0/235), 상한 1.3% | 0.0% (0/235), 상한 1.3% |
| 정상(CLEAN) 오탐률 | 0.3% (1/301) | 0.0% (0/301), 상한 1.0% |
| 경계(AMBIGUOUS) PENDING 비율 | 0.0% (0/43), 상한 7.0% | 0.0% (0/43), 상한 7.0% |
| 경계 판정 분포 CLEAN / FLAGGED / PENDING | 21 / 22 / 0 | 29 / 14 / 0 |
| 회차별 MISSING | 0 / 0 / 0 | 0 / 250 / 0 |
| 성공한 호출 응답 시간 평균 / 최대 | 72.4s / 200.6s (9회) | 19.3s / 28.3s (8회) |
| 실패한 호출 | 0.0% (0/9), 상한 33.3% | 11.1% (1/9) |
| 호출당 토큰 평균 (입력 / 출력 / 캐시 / 생각) | 기록 없음 | 기록 없음 |
| 반복 간 판정 뒤집힘 | 1.2% (7/579) | 1.2% (7/579) |

### 반복 흔들림과 모델 간 차이

회차 두 개를 골라 판정이 다른 닉네임 비율을 재고 쌍 평균을 냈다.

| 비교 | 판정 불일치율 |
| --- | --- |
| gemini-3.8-flash 회차끼리 | 0.86% |
| gemini-3.5-flash-lite 회차끼리 | 1.19% |
| gemini-3.8-flash 회차와 gemini-3.5-flash-lite 회차 | 1.70% |

### 판정 일치도

다수결 판정 579건에서 일치 98.4%, Cohen's κ = 0.968.

| gemini-3.8-flash \ gemini-3.5-flash-lite | CLEAN | FLAGGED | PENDING |
| --- | --- | --- | --- |
| CLEAN | 321 | 0 | 0 |
| FLAGGED | 9 | 249 | 0 |
| PENDING | 0 | 0 | 0 |

### McNemar 정확 검정 (정답 맞음/틀림)

b 는 gemini-3.8-flash 만 맞은 건수, c 는 gemini-3.5-flash-lite 만 맞은 건수다. 차이는 gemini-3.5-flash-lite 정답률에서 gemini-3.8-flash 정답률을 뺀 값이다.

| 범위 | n | 둘 다 맞음 | b | c | 둘 다 틀림 | p (양측) | 정답률 차이 (95% 구간) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 전체 | 579 | 535 | 0 | 1 | 43 | 1.000 | +0.17%p (-0.17 ~ +0.51) |
| 경계 제외 | 536 | 535 | 0 | 1 | 0 | 1.000 | +0.19%p (-0.18 ~ +0.55) |
| 욕설·우회만 | 235 | 235 | 0 | 0 | 0 | 1.000 | +0.00%p (-1.28 ~ +1.28) |
| 정상만 | 301 | 300 | 0 | 1 | 0 | 1.000 | +0.33%p (-0.32 ~ +0.98) |

비열등 한계 -2%p 기준: 전체 범위 구간 하한이 -0.17%p 라서 gemini-3.5-flash-lite 는 gemini-3.8-flash 보다 비열등하다.

### 카테고리별

걸러냄은 FLAGGED 와 PENDING 을 합친 비율이다. 욕설·우회 행에서는 재현율이고 정상 행에서는 오탐률이다.

| 카테고리 | expected | 걸러냄 gemini-3.8-flash | 걸러냄 gemini-3.5-flash-lite | PENDING gemini-3.8-flash | PENDING gemini-3.5-flash-lite |
| --- | --- | --- | --- | --- | --- |
| 직접욕설 | PROFANE | 100.0% (30/30) | 100.0% (30/30) | 0/30 | 0/30 |
| 이름+욕설 | PROFANE | 100.0% (20/20) | 100.0% (20/20) | 0/20 | 0/20 |
| 자모분리 | EVASION | 100.0% (15/15) | 100.0% (15/15) | 0/15 | 0/15 |
| 특수문자삽입 | EVASION | 100.0% (15/15) | 100.0% (15/15) | 0/15 | 0/15 |
| 유사발음 | EVASION | 100.0% (20/20) | 100.0% (20/20) | 0/20 | 0/20 |
| 초성 | EVASION | 100.0% (15/15) | 100.0% (15/15) | 0/15 | 0/15 |
| 경계 | AMBIGUOUS | 51.2% (22/43) | 32.6% (14/43) | 0/43 | 0/43 |
| 오탐유발정상어 | CLEAN | 0.0% (0/73), 상한 4.1% | 0.0% (0/73), 상한 4.1% | 0/73 | 0/73 |
| 일반닉네임 | CLEAN | 0.0% (0/170), 상한 1.8% | 0.0% (0/170), 상한 1.8% | 0/170 | 0/170 |
| 복합우회 | EVASION | 100.0% (15/15) | 100.0% (15/15) | 0/15 | 0/15 |
| 영타 | EVASION | 100.0% (9/9) | 100.0% (9/9) | 0/9 | 0/9 |
| 로마자 | EVASION | 100.0% (10/10) | 100.0% (10/10) | 0/10 | 0/10 |
| 늘이기 | EVASION | 100.0% (10/10) | 100.0% (10/10) | 0/10 | 0/10 |
| 변형표기 | EVASION | 100.0% (20/20) | 100.0% (20/20) | 0/20 | 0/20 |
| 유사문자 | EVASION | 100.0% (12/12) | 100.0% (12/12) | 0/12 | 0/12 |
| 공백삽입 | EVASION | 100.0% (6/6) | 100.0% (6/6) | 0/6 | 0/6 |
| 영어변형 | EVASION | 100.0% (9/9) | 100.0% (9/9) | 0/9 | 0/9 |
| 영어욕설 | PROFANE | 100.0% (5/5) | 100.0% (5/5) | 0/5 | 0/5 |
| 혐오비하 | PROFANE | 100.0% (15/15) | 100.0% (15/15) | 0/15 | 0/15 |
| 성적표현 | PROFANE | 100.0% (9/9) | 100.0% (9/9) | 0/9 | 0/9 |
| 경계걸침정상어 | CLEAN | 2.4% (1/42) | 0.0% (0/42), 상한 7.1% | 0/42 | 0/42 |
| 정상초성 | CLEAN | 0.0% (0/16), 상한 18.8% | 0.0% (0/16), 상한 18.8% | 0/16 | 0/16 |

## 한계

- **11-13 에는 두 모델을 비교하지 못했다.** flash 호출 12회가 모두 실패했다. 하드셋과 순서를 섞은 579건의 결과는 flash-lite 혼자 잰 값이라 "flash 보다 나쁜가"에는 답하지 못한다.
- **11-13 의 다수결은 표가 모자란다.** 503 실패 4회 때문에 하드셋은 179건(51.1%)이 판정 1회, 142건이 2회다. 579건은 120건이 1회, 261건이 2회다. 뒤집힘 17.0% 인 세트에서 판정 1회로 정한 값은 다시 재면 바뀔 수 있다.
- **순서 효과는 가르지 못했다.** 11-11 과 11-13 은 날짜, 순서, 표본별 판정 수가 모두 다르다. 시드도 1907 하나만 썼다. 오탐이 0건에서 7건으로 늘어난 것이 순서 때문인지 날짜에 따른 모델 흔들림인지 이 자료로는 구분되지 않는다.
- **하드셋 라벨은 사람이 달지 않았다.** 언어 모델로 두 번 라벨링했고 두 라벨의 κ 는 0.920 이다. 혐오차별과 집단비하 축은 두 라벨의 일치율이 73.1% 다. 이 두 축의 재현율 65.0% 와 60.0% 는 모델이 틀린 것인지 라벨이 틀린 것인지 가를 수 없다.
- **하드셋은 축마다 표본이 적다.** 우회 축은 18건씩이라 미탐 0건이어도 95% 신뢰 상한이 16.7% 다. 집단비하의 욕설·우회 표본은 5건뿐이다.
- **11-11 결과는 천장 효과 아래에 있다.** 두 모델 모두 욕설과 우회를 100% 걸러냈고, 그 측정은 순서를 고정했다. 11-13 에 같은 세트에서 flash-lite 의 오탐과 뒤집힘이 늘었으므로 11-11 의 "오탐 0건"은 조건이 바뀌면 유지되지 않는다.
- **골든셋 분포는 운영 분포가 아니다.** 579건 중 욕설과 우회가 40.6% 이고, 하드셋은 일부러 어려운 표본만 모았다. 운영 닉네임은 대부분 정상이라 여기서 잰 비율이 운영 오탐률이나 미탐률은 아니다.
- **비열등 한계 2%p 는 잠정값이다.** 운영 근거 없이 정한 숫자이고 사용자 확인이 필요하다.
- **confidence 는 소수 둘째 자리로 반올림된 값이다.** 운영 코드의 `AiConfidence` 가 그렇게 저장한다. 원값이 0.845~0.85 사이인 행은 0.85 로 올라가 FLAGGED 가 된다.
- **토큰 기록에서 캐시와 생각 토큰은 모두 0 이었다.** flash-lite 응답의 `cachedContentTokenCount` 와 `thoughtsTokenCount` 가 비어 있었다. flash 는 성공한 호출이 없어 기록이 없다.

## 다음 측정

flash 가 응답하는 때에 같은 조건으로 flash 두 세트를 잰다. 호출은 15회다(하드셋 6회, 579건 9회). flash-lite 도 503 으로 빈 4개 배치를 채우려면 같은 시드로 15회를 다시 재야 한다. 순서 효과를 가르려면 같은 날 순서 고정과 순서 섞음을 번갈아 돌린다.

## 재현 방법

행 단위 판정과 호출 기록은 `data/` 에 있다.

| 파일 | 내용 |
| --- | --- |
| `1907-golden-gemini-3.8-flash-timeout300.tsv` | 11-11 flash 579건, 순서 고정 |
| `1907-golden-gemini-3.8-flash-timeout120.tsv` | 11-11 flash 579건, 타임아웃 120초라 4개 배치가 비었다 |
| `1907-golden-gemini-3.5-flash-lite.tsv` | 11-11 flash-lite 579건, 순서 고정 |
| `1907-golden-shuffled-gemini-3.5-flash-lite.tsv`, `.calls.tsv` | 11-13 flash-lite 579건, 순서 섞음 |
| `1907-hard-shuffled-gemini-3.5-flash-lite.tsv`, `.calls.tsv` | 11-13 flash-lite 하드셋 |
| `1907-hard-shuffled-gemini-3.8-flash-failed-1.calls.tsv`, `-2.calls.tsv` | 11-13 flash 실패 12회의 호출 기록 |

```bash
cd backend
# 모델 하나의 지표, 표본별 판정 수, confidence 분포
python3 profanity/scripts/compare_models.py --single gemini-3.5-flash-lite \
  docs/eval/data/1907-hard-shuffled-gemini-3.5-flash-lite.tsv
# 두 측정의 짝 비교 (11-11 두 모델, 또는 같은 모델의 순서 고정과 순서 섞음)
python3 profanity/scripts/compare_models.py \
  gemini-3.8-flash docs/eval/data/1907-golden-gemini-3.8-flash-timeout300.tsv \
  gemini-3.5-flash-lite docs/eval/data/1907-golden-gemini-3.5-flash-lite.tsv
```

다시 재려면 세트와 모델마다 러너를 돌린다. TSV 는 `profanity/build/reports/golden/` 에 남는다.

```bash
cd backend
GEMINI_API_KEY=... NICKNAME_GOLDEN_SET=hard NICKNAME_AUDIT_MODEL=gemini-3.8-flash \
  NICKNAME_AUDIT_BATCH_SIZE=250 NICKNAME_AUDIT_REPEATS=3 NICKNAME_AUDIT_HTTP_ATTEMPTS=1 \
  NICKNAME_AUDIT_TIMEOUT_SECONDS=300 NICKNAME_AUDIT_SHUFFLE_SEED=1907 \
  ./gradlew :profanity:goldenTest
```

## 부록: 11-13 579건에서 다수결이 틀렸거나 판정이 갈린 표본

회차별 칸은 판정, 배치 번호와 배치 안 위치, confidence 순이다. 경계 표본은 PENDING 이 아니면 틀린 것으로 센다.

| 닉네임 | expected | 카테고리 | 다수결 | 1회차 | 2회차 | 3회차 |
| --- | --- | --- | --- | --- | --- | --- |
| 엠창인생 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-7, 0.99) | FLAGGED (3-69, 0.98) | FLAGGED (2-121, 0.99) |
| 불알친구 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-8, 0.90) | MISSING | CLEAN (2-233, 0.85) |
| 바보멍청이 | AMBIGUOUS | 경계 | CLEAN | CLEAN (1-14, 0.95) | CLEAN (2-25, 0.90) | CLEAN (2-63, 0.95) |
| 졸라멋짐 | AMBIGUOUS | 경계 | CLEAN | CLEAN (1-16, 0.85) | MISSING | CLEAN (3-26, 0.80) |
| 또라이 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-19, 0.95) | FLAGGED (3-7, 0.95) | FLAGGED (1-178, 0.85) |
| 시발택시 | CLEAN | 오탐유발정상어 | FLAGGED | FLAGGED (1-25, 0.99) | FLAGGED (2-43, 0.99) | FLAGGED (2-34, 0.97) |
| 수박씨발라 | CLEAN | 경계걸침정상어 | FLAGGED | FLAGGED (1-31, 0.95) | CLEAN (2-106, 0.90) | FLAGGED (1-47, 0.90) |
| 돌아이 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-41, 0.95) | MISSING | CLEAN (2-39, 0.85) |
| 상병신고식 | CLEAN | 경계걸침정상어 | FLAGGED | FLAGGED (1-47, 0.99) | MISSING | FLAGGED (2-58, 0.98) |
| whssk | EVASION | 영타 | FLAGGED | FLAGGED (1-50, 0.95) | MISSING | CLEAN (3-44, 0.80) |
| 병맛인생 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-61, 0.95) | MISSING | CLEAN (2-202, 0.85) |
| rotoRl | EVASION | 영타 | FLAGGED | CLEAN (1-75, 0.99) | FLAGGED (3-70, 0.90) | FLAGGED (1-70, 0.95) |
| 지우엿먹어 | PROFANE | 이름+욕설 | CLEAN | CLEAN (1-78, 0.90) | FLAGGED (2-65, 0.90) | PENDING (1-139, 0.80) |
| 호랑이새끼 | CLEAN | 경계걸침정상어 | CLEAN | CLEAN (1-81, 0.85) | MISSING | PENDING (1-194, 0.70) |
| 똥남아 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-89, 0.95) | MISSING | FLAGGED (1-83, 0.99) |
| 씹고수 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-92, 0.90) | FLAGGED (2-136, 0.90) | CLEAN (1-204, 0.80) |
| 존버중 | AMBIGUOUS | 경계 | CLEAN | CLEAN (1-95, 0.90) | MISSING | CLEAN (1-113, 0.90) |
| 죽어버려 | AMBIGUOUS | 경계 | CLEAN | CLEAN (1-114, 0.85) | FLAGGED (2-91, 0.95) | PENDING (1-9, 0.80) |
| 엿먹어라 | PROFANE | 직접욕설 | FLAGGED | CLEAN (1-119, 0.90) | FLAGGED (2-45, 0.90) | FLAGGED (3-40, 0.95) |
| 끼새개 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-134, 0.95) | FLAGGED (2-188, 0.95) | FLAGGED (1-98, 0.95) |
| 개웃겨 | AMBIGUOUS | 경계 | CLEAN | CLEAN (1-137, 0.90) | MISSING | CLEAN (1-52, 0.95) |
| 개못함 | AMBIGUOUS | 경계 | CLEAN | CLEAN (1-155, 0.85) | CLEAN (3-24, 0.85) | CLEAN (2-218, 0.90) |
| 놈발씨 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-156, 0.95) | FLAGGED (2-51, 0.90) | FLAGGED (2-195, 0.90) |
| 고양이새끼 | CLEAN | 경계걸침정상어 | CLEAN | CLEAN (1-169, 0.85) | FLAGGED (2-162, 0.90) | PENDING (1-238, 0.70) |
| 개꿀잼 | AMBIGUOUS | 경계 | CLEAN | CLEAN (1-201, 0.90) | CLEAN (3-43, 0.90) | CLEAN (1-36, 0.95) |
| wtf | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-212, 0.95) | MISSING | FLAGGED (3-56, 0.95) |
| 멸치대가리 | AMBIGUOUS | 경계 | FLAGGED | CLEAN (1-216, 0.85) | FLAGGED (2-74, 0.85) | FLAGGED (2-24, 0.85) |
| 강아지새끼 | CLEAN | 경계걸침정상어 | FLAGGED | CLEAN (1-236, 0.80) | FLAGGED (2-64, 0.99) | FLAGGED (1-206, 0.90) |
| 자살각 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-239, 0.85) | FLAGGED (2-159, 0.90) | CLEAN (3-63, 0.70) |
| 빡빡이 | AMBIGUOUS | 경계 | CLEAN | CLEAN (1-249, 0.90) | CLEAN (2-202, 0.90) | CLEAN (3-15, 0.90) |
| 꺼져 | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (2-150, 0.90) | FLAGGED (2-230, 0.95) |
| 꽃씨발아 | CLEAN | 경계걸침정상어 | FLAGGED | MISSING | FLAGGED (2-215, 0.85) | FLAGGED (1-213, 0.90) |
| 시발역 | CLEAN | 경계걸침정상어 | FLAGGED | MISSING | FLAGGED (2-192, 0.90) | CLEAN (1-76, 0.85) |
| 닥쳐라 | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (2-88, 0.90) | FLAGGED (2-179, 0.95) |
| 개노잼 | AMBIGUOUS | 경계 | CLEAN | MISSING | CLEAN (2-90, 0.90) | CLEAN (1-2, 0.90) |
| 병신년 | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (3-46, 0.85) | FLAGGED (1-133, 0.99) |
| 똥강아지 | AMBIGUOUS | 경계 | CLEAN | MISSING | CLEAN (2-176, 0.90) | CLEAN (3-51, 0.90) |
| 뒤져라 | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (2-26, 0.90) | FLAGGED (2-42, 0.95) |
| 미친개 | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (3-21, 0.90) | CLEAN (2-231, 0.80) |
| 변태가면 | AMBIGUOUS | 경계 | CLEAN | MISSING | CLEAN (2-171, 0.90) | CLEAN (2-32, 0.90) |
| stfu | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (2-7, 0.99) | FLAGGED (1-94, 0.99) |
| 존잘남 | AMBIGUOUS | 경계 | CLEAN | MISSING | MISSING | CLEAN (1-243, 0.90) |
| 야동마스터 | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (3-12, 0.90) | FLAGGED (1-17, 0.95) |
| 존맛탱 | AMBIGUOUS | 경계 | CLEAN | MISSING | MISSING | CLEAN (1-63, 0.95) |
| 개빡침 | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (2-122, 0.90) | CLEAN (2-54, 0.80) |
| 씨밤바 | AMBIGUOUS | 경계 | FLAGGED | MISSING | MISSING | FLAGGED (3-46, 0.99) |
| 섹시한남자 | AMBIGUOUS | 경계 | CLEAN | MISSING | CLEAN (2-146, 0.95) | CLEAN (2-10, 0.95) |
| 일병신고 | CLEAN | 경계걸침정상어 | FLAGGED | MISSING | MISSING | FLAGGED (3-21, 0.95) |
| 호구왕 | AMBIGUOUS | 경계 | CLEAN | MISSING | CLEAN (2-79, 0.85) | CLEAN (2-186, 0.85) |
| 찐따 | AMBIGUOUS | 경계 | FLAGGED | MISSING | FLAGGED (2-119, 0.90) | FLAGGED (2-118, 0.90) |
| mofo | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (3-40, 0.95) | MISSING | FLAGGED (1-90, 0.99) |
| 흑형 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (3-50, 0.85) | MISSING | CLEAN (2-220, 0.85) |
| 조낸멋짐 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (3-58, 0.85) | FLAGGED (2-186, 0.90) | CLEAN (1-184, 0.80) |
| 미친듯이 | AMBIGUOUS | 경계 | CLEAN | CLEAN (3-71, 0.95) | CLEAN (2-72, 0.95) | CLEAN (1-26, 0.95) |

## 부록: 11-13 하드셋에서 다수결이 틀렸거나 판정이 갈린 표본

회차별 칸은 판정, 배치 번호와 배치 안 위치, confidence 순이다. 경계 표본은 PENDING 이 아니면 틀린 것으로 센다.

| 닉네임 | expected | 카테고리 | 다수결 | 1회차 | 2회차 | 3회차 |
| --- | --- | --- | --- | --- | --- | --- |
| 지균충 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (2-40, 0.95) | FLAGGED (2-86, 0.85) |
| 찌질이 | AMBIGUOUS | 애매 | FLAGGED | MISSING | FLAGGED (1-132, 0.85) | MISSING |
| 빨아줄래 | AMBIGUOUS | 성적표현 | FLAGGED | MISSING | FLAGGED (1-161, 0.90) | MISSING |
| 조ㄴ나웃김 | EVASION | 자모분리 | CLEAN | MISSING | CLEAN (1-85, 0.90) | MISSING |
| 밤일잘해요 | AMBIGUOUS | 성적표현 | CLEAN | MISSING | CLEAN (2-52, 0.80) | MISSING |
| 외노자 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-10, 0.80) | MISSING |
| ㅇㄴㅇ구함 | EVASION | 성적표현 | CLEAN | MISSING | CLEAN (1-11, 0.90) | MISSING |
| 폐급신병 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (1-175, 0.85) | MISSING |
| simp | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-142, 0.85) | MISSING |
| 우라질 | AMBIGUOUS | 애매 | FLAGGED | MISSING | FLAGGED (1-204, 0.90) | FLAGGED (2-18, 0.90) |
| 상폐女 | AMBIGUOUS | 혐오차별 | FLAGGED | MISSING | FLAGGED (2-84, 0.90) | PENDING (2-4, 0.80) |
| 개같이부활 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-197, 0.95) | CLEAN (2-82, 0.90) |
| 수급자거지 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (1-123, 0.95) | MISSING |
| 쪼다 | AMBIGUOUS | 애매 | FLAGGED | MISSING | FLAGGED (1-114, 0.85) | MISSING |
| rotorrl | EVASION | 한영오타 | CLEAN | MISSING | CLEAN (2-95, 0.95) | CLEAN (2-87, 0.70) |
| 짱개집 | PROFANE | 혐오차별 | CLEAN | MISSING | CLEAN (2-7, 0.90) | MISSING |
| 콩나물대가리 | CLEAN | 하드네거티브 | FLAGGED | MISSING | FLAGGED (1-112, 0.85) | MISSING |
| 자지러지는중 | CLEAN | 하드네거티브 | FLAGGED | MISSING | FLAGGED (1-173, 0.95) | MISSING |
| 이건니거야 | CLEAN | 하드네거티브 | FLAGGED | MISSING | FLAGGED (1-139, 0.85) | MISSING |
| 박아줄게 | AMBIGUOUS | 성적표현 | FLAGGED | MISSING | FLAGGED (1-41, 0.90) | FLAGGED (2-3, 0.85) |
| 설거지남 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (1-138, 0.85) | PENDING (2-34, 0.75) |
| 꼬추왕 | AMBIGUOUS | 애매 | FLAGGED | MISSING | FLAGGED (2-2, 0.95) | MISSING |
| 젠신병자 | AMBIGUOUS | 혐오차별 | FLAGGED | MISSING | FLAGGED (2-83, 0.95) | FLAGGED (2-21, 0.85) |
| 빌어먹을 | AMBIGUOUS | 애매 | FLAGGED | MISSING | FLAGGED (1-235, 0.85) | MISSING |
| 19금 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-167, 0.90) | CLEAN (2-15, 0.90) |
| 학식충 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (1-90, 0.90) | MISSING |
| 물많은누나 | AMBIGUOUS | 성적표현 | FLAGGED | MISSING | FLAGGED (1-69, 0.90) | MISSING |
| 이시키 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (2-66, 0.85) | MISSING |
| qbdtls | EVASION | 한영오타 | CLEAN | MISSING | CLEAN (2-57, 0.95) | CLEAN (2-16, 0.70) |
| 빠순이 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (1-66, 0.85) | MISSING |
| 꿀벅지 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (2-89, 0.85) | MISSING |
| 귀머거리냐 | AMBIGUOUS | 혐오차별 | CLEAN | MISSING | CLEAN (1-50, 0.85) | MISSING |
| Tidsus | EVASION | 한영오타 | CLEAN | MISSING | CLEAN (1-153, 0.99) | MISSING |
| 발기찬하루 | AMBIGUOUS | 성적표현 | FLAGGED | MISSING | FLAGGED (2-37, 0.85) | MISSING |
| 흡연충 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (1-203, 0.90) | FLAGGED (2-71, 0.85) |
| 편돌이주제 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (1-219, 0.85) | MISSING |
| 개저씨싫어 | AMBIGUOUS | 혐오차별 | FLAGGED | MISSING | FLAGGED (1-143, 0.90) | MISSING |
| 오피추천좀 | AMBIGUOUS | 성적표현 | FLAGGED | MISSING | FLAGGED (1-188, 0.95) | FLAGGED (2-83, 0.85) |
| 할매미 | AMBIGUOUS | 혐오차별 | CLEAN | MISSING | CLEAN (1-9, 0.90) | MISSING |
| 모지리 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-144, 0.80) | CLEAN (2-36, 0.80) |
| 소추소심 | AMBIGUOUS | 혐오차별 | FLAGGED | MISSING | FLAGGED (1-92, 0.90) | MISSING |
| 퐁퐁남등장 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (2-72, 0.85) | MISSING |
| 나는쓰레기 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (2-62, 0.95) | MISSING |
| 빨아줘요 | AMBIGUOUS | 애매 | FLAGGED | MISSING | FLAGGED (2-86, 0.85) | MISSING |
| 까보전 | AMBIGUOUS | 혐오차별 | FLAGGED | MISSING | FLAGGED (1-108, 0.95) | FLAGGED (2-61, 0.85) |
| 싸고싶다 | AMBIGUOUS | 애매 | FLAGGED | MISSING | FLAGGED (1-111, 0.90) | MISSING |
| 십장생 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-151, 0.99) | MISSING |
| 2백충 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (1-31, 0.95) | MISSING |
| 벙어리 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-14, 0.90) | MISSING |
| 틀니압수 | AMBIGUOUS | 혐오차별 | FLAGGED | MISSING | FLAGGED (1-96, 0.85) | MISSING |
| doan장인 | EVASION | 성적표현 | CLEAN | MISSING | CLEAN (2-38, 0.90) | MISSING |
| 개망나니 | AMBIGUOUS | 애매 | FLAGGED | MISSING | FLAGGED (1-199, 0.85) | MISSING |
| ㄱㅈㅇ | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-125, 0.70) | MISSING |
| milf | PROFANE | 성적표현 | FLAGGED | MISSING | FLAGGED (1-163, 0.90) | PENDING (2-69, 0.80) |
| 젠장할 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (2-60, 0.90) | MISSING |
| thicc | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-207, 0.85) | CLEAN (2-43, 0.90) |
| 시골촌놈 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-75, 0.90) | MISSING |
| ㅈㄴㄱㄷ | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-231, 0.90) | CLEAN (2-26, 0.80) |
| 월거지탈출 | AMBIGUOUS | 집단비하 | FLAGGED | MISSING | FLAGGED (2-8, 0.85) | MISSING |
| ehlswkdsu | EVASION | 혐오차별 | CLEAN | MISSING | CLEAN (2-96, 0.95) | MISSING |
| 제기랄 | AMBIGUOUS | 애매 | CLEAN | MISSING | CLEAN (1-49, 0.90) | MISSING |
| 밝히는남자 | AMBIGUOUS | 애매 | PENDING | MISSING | PENDING (1-158, 0.80) | CLEAN (2-75, 0.80) |
| 원숭이섬나라 | PROFANE | 혐오차별 | CLEAN | MISSING | CLEAN (2-78, 0.95) | CLEAN (2-17, 0.99) |
| 파5후쿰척 | AMBIGUOUS | 집단비하 | CLEAN | MISSING | CLEAN (1-189, 0.90) | PENDING (2-80, 0.70) |
| 꽃병신상 | CLEAN | 하드네거티브 | FLAGGED | FLAGGED (2-1, 0.95) | FLAGGED (1-74, 0.95) | FLAGGED (2-39, 0.95) |
| 짱깨끗해 | CLEAN | 하드네거티브 | FLAGGED | FLAGGED (2-2, 0.85) | CLEAN (2-4, 0.85) | MISSING |
| 호모 | AMBIGUOUS | 애매 | CLEAN | CLEAN (2-5, 0.70) | FLAGGED (1-239, 0.85) | PENDING (2-65, 0.70) |
| 기레기척결 | AMBIGUOUS | 집단비하 | CLEAN | CLEAN (2-7, 0.85) | CLEAN (1-102, 0.85) | MISSING |
| Tkdsha | EVASION | 한영오타 | CLEAN | CLEAN (2-10, 0.99) | FLAGGED (2-30, 0.98) | MISSING |
| 도랏맨 | AMBIGUOUS | 애매 | CLEAN | CLEAN (2-11, 0.90) | CLEAN (1-39, 0.90) | CLEAN (2-42, 0.90) |
| 쉬바 | AMBIGUOUS | 애매 | FLAGGED | FLAGGED (2-17, 0.95) | FLAGGED (1-19, 0.98) | MISSING |
| 탈모충 | AMBIGUOUS | 집단비하 | FLAGGED | FLAGGED (2-18, 0.95) | FLAGGED (2-93, 0.98) | MISSING |
| 존예보스 | AMBIGUOUS | 애매 | CLEAN | CLEAN (2-19, 0.90) | CLEAN (2-94, 0.85) | MISSING |
| 쥰내싫어 | EVASION | 발음변형 | CLEAN | CLEAN (2-21, 0.85) | CLEAN (1-209, 0.85) | MISSING |
| 가슴만지자 | PROFANE | 성적표현 | CLEAN | CLEAN (2-23, 0.80) | FLAGGED (1-73, 0.95) | MISSING |
| G잡대출신 | AMBIGUOUS | 집단비하 | CLEAN | CLEAN (2-28, 0.80) | FLAGGED (1-174, 0.85) | MISSING |
| 이등신 | AMBIGUOUS | 애매 | CLEAN | CLEAN (2-29, 0.90) | CLEAN (1-162, 0.85) | MISSING |
| 犬찰아웃 | AMBIGUOUS | 집단비하 | CLEAN | CLEAN (2-31, 0.75) | FLAGGED (1-70, 0.90) | MISSING |
| 된장녀 | PROFANE | 혐오차별 | CLEAN | CLEAN (2-33, 0.80) | FLAGGED (2-67, 0.90) | MISSING |
| 도태남집합 | AMBIGUOUS | 집단비하 | FLAGGED | CLEAN (2-36, 0.85) | FLAGGED (1-170, 0.90) | FLAGGED (2-78, 0.85) |
| 노팬티 | AMBIGUOUS | 애매 | CLEAN | CLEAN (2-40, 0.85) | CLEAN (2-100, 0.85) | MISSING |
| 시바ㄹ점 | AMBIGUOUS | 자모분리 | FLAGGED | FLAGGED (2-41, 0.95) | FLAGGED (1-99, 0.95) | MISSING |
| 빨갱이 | PROFANE | 집단비하 | CLEAN | CLEAN (2-43, 0.80) | CLEAN (1-20, 0.85) | MISSING |
| badass | AMBIGUOUS | 애매 | CLEAN | CLEAN (2-45, 0.95) | CLEAN (2-10, 0.90) | CLEAN (2-58, 0.90) |
| 야스각 | EVASION | 성적표현 | CLEAN | CLEAN (2-47, 0.80) | FLAGGED (1-228, 0.85) | MISSING |
| alclssus | EVASION | 한영오타 | CLEAN | CLEAN (2-52, 0.90) | FLAGGED (2-26, 0.98) | CLEAN (2-90, 0.70) |
| 개쌍도출신 | PROFANE | 혐오차별 | CLEAN | CLEAN (2-55, 0.80) | FLAGGED (1-106, 0.90) | MISSING |
| 노가다꾼주제 | AMBIGUOUS | 집단비하 | CLEAN | CLEAN (2-58, 0.85) | FLAGGED (2-76, 0.85) | PENDING (2-67, 0.80) |
| dutajrdj | EVASION | 한영오타 | CLEAN | CLEAN (2-65, 0.90) | CLEAN (1-137, 0.95) | MISSING |
| pen_island | AMBIGUOUS | 애매 | CLEAN | CLEAN (2-67, 0.99) | CLEAN (1-64, 0.99) | MISSING |
| ㄱㅅ77 | EVASION | 초성숫자혼합 | FLAGGED | FLAGGED (2-72, 0.85) | FLAGGED (1-233, 0.99) | CLEAN (2-77, 0.50) |
| wl.f kf하네 | EVASION | 복합삽입 | FLAGGED | FLAGGED (2-77, 0.95) | FLAGGED (1-248, 0.99) | CLEAN (2-73, 0.60) |
| 발정난토끼 | AMBIGUOUS | 성적표현 | CLEAN | CLEAN (2-78, 0.80) | FLAGGED (1-34, 0.85) | MISSING |
| sldoal | EVASION | 한영오타 | CLEAN | CLEAN (2-80, 0.90) | FLAGGED (2-70, 0.98) | MISSING |
| 꼴페미척결 | PROFANE | 혐오차별 | CLEAN | CLEAN (2-82, 0.85) | FLAGGED (1-238, 0.90) | MISSING |
| 짭새단속 | AMBIGUOUS | 집단비하 | CLEAN | CLEAN (2-83, 0.85) | FLAGGED (1-47, 0.90) | PENDING (2-32, 0.80) |
| 외노자꺼져 | PROFANE | 집단비하 | CLEAN | CLEAN (2-84, 0.80) | FLAGGED (1-185, 0.90) | MISSING |
| 여자는부엌 | PROFANE | 혐오차별 | CLEAN | CLEAN (2-85, 0.80) | FLAGGED (1-51, 0.90) | MISSING |
| 모쏠아다 | AMBIGUOUS | 집단비하 | CLEAN | CLEAN (2-90, 0.80) | FLAGGED (1-109, 0.85) | MISSING |
| ghfhtoRl | EVASION | 한영오타 | CLEAN | CLEAN (2-92, 0.90) | CLEAN (2-81, 0.90) | MISSING |
| 땡중 | AMBIGUOUS | 집단비하 | CLEAN | CLEAN (2-95, 0.85) | FLAGGED (1-104, 0.85) | MISSING |
| 군ㅂㅏ리 | AMBIGUOUS | 집단비하 | CLEAN | CLEAN (2-96, 0.85) | FLAGGED (1-246, 0.85) | MISSING |
| 인간말종 | AMBIGUOUS | 애매 | CLEAN | CLEAN (2-99, 0.85) | FLAGGED (1-15, 0.98) | MISSING |

## 부록: 11-11 두 모델의 다수결 판정이 엇갈린 표본

| 닉네임 | expected | 카테고리 | gemini-3.8-flash (회차별) | gemini-3.5-flash-lite (회차별) |
| --- | --- | --- | --- | --- |
| 병신년 | AMBIGUOUS | 경계 | FLAGGED (FLAGGED/FLAGGED/FLAGGED) | CLEAN (CLEAN/MISSING/FLAGGED) |
| 멸치대가리 | AMBIGUOUS | 경계 | FLAGGED (FLAGGED/CLEAN/FLAGGED) | CLEAN (CLEAN/MISSING/CLEAN) |
| 씹덕 | AMBIGUOUS | 경계 | FLAGGED (FLAGGED/FLAGGED/PENDING) | CLEAN (CLEAN/MISSING/CLEAN) |
| 찐따 | AMBIGUOUS | 경계 | FLAGGED (FLAGGED/FLAGGED/FLAGGED) | CLEAN (CLEAN/MISSING/CLEAN) |
| 또라이 | AMBIGUOUS | 경계 | FLAGGED (FLAGGED/FLAGGED/FLAGGED) | CLEAN (CLEAN/MISSING/CLEAN) |
| 돌아이 | AMBIGUOUS | 경계 | FLAGGED (FLAGGED/FLAGGED/FLAGGED) | CLEAN (CLEAN/MISSING/CLEAN) |
| 미친개 | AMBIGUOUS | 경계 | FLAGGED (FLAGGED/FLAGGED/FLAGGED) | CLEAN (CLEAN/MISSING/CLEAN) |
| 흑형 | AMBIGUOUS | 경계 | FLAGGED (FLAGGED/PENDING/FLAGGED) | CLEAN (CLEAN/CLEAN/CLEAN) |
| 아저씨발냄새 | CLEAN | 경계걸침정상어 | FLAGGED (FLAGGED/PENDING/CLEAN) | CLEAN (CLEAN/CLEAN/CLEAN) |
