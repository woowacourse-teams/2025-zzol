# 포스트모템 인덱스

장애·인시던트·반복된 오진의 사후 회고 목록. 새 포스트모템 작성은 `/postmortem [사건]`으로 한다.

ADR이 "앞으로 무엇을 할지"의 결정 기록이라면, 포스트모템은 "무엇이 어떻게 터졌고 왜 그렇게 진단했는가"의 복기 기록이다. 회고에서 도출한 결정은 별도 ADR로 분리하고, 양쪽을 상호 링크한다. 포스트모템 번호는 ADR과 독립된 시퀀스로 0001부터 매긴다.

심각도 기준: **P0** 사용자 영향 있는 프로덕션 장애 / **P1** 배포·운영 중단 또는 데이터 위험 / **P2** 개발·CI 생산성 저하(사용자 영향 없음).

| 번호 | 제목 | 날짜 | 심각도 | 근본 원인 (한 줄) | 관련 ADR |
|------|------|------|--------|------------------|----------|
| [0001](0001-game-integration-test-flaky.md) | 게임 통합테스트 플레이키 — poison-message 폭풍과 스케줄러 기아 오진 | 2026-06-11 | P2 | 공유 컨테이너 reuse + 멀티모듈 동시실행으로 stale 이벤트가 공유 스트림 컨슈머 풀을 포화시켜 게임 이벤트 처리가 기아 상태에 빠짐 | [0013](../adr/0013-domain-module-test-isolation.md) |
| [0002](0002-testcontainers-reuse-decision-reversal.md) | Testcontainers reuse 의사결정 2회 번복 | 2026-06-18 | P2 | 근본 원인을 reuse로 오귀속해 격리를 제거했으나 실제 수정은 타이밍 결함(#1411)이었고, 변경 변수를 통제하지 못해 복구 시도(#1418)마저 revert | [0013](../adr/0013-domain-module-test-isolation.md) |
| [0003](0003-test-mirror-checklist-incomplete-recurrence.md) | 테스트 미러링 체크리스트 불완전 — 문서화 후 재발 | 2026-06-22 | P2 | 새 게임 스케줄러 빈 미러를 빠뜨려 app IT 약 55건이 컨텍스트 로딩 실패. 1차 사고 후 작성한 체크리스트가 미러 3곳 중 1곳만 열거해 문서를 따랐는데도 재발 | [0031](../adr/0031-nunchi-game-design.md) |
| [0003](0003-internal-ip-block-dev-webhook.md) | 내부 IP 차단 — dev webhook 테스트가 prod 리스너로 유입 + 화이트리스트 유실 | 2026-06-23 | P1 | 내부 리스너 환경 미분리로 dev 테스트가 prod-app 404를 유발했고, #1352 내부 IP 화이트리스트가 머지 안 돼 가드 부재로 nginx 내부 IP가 누적 차단됨 | [0032](../adr/0032-zzolbot-as-alertmanager-receiver.md) |
