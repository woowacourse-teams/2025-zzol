# create-pr 작성 예시

[SKILL.md](SKILL.md)에서 분리한 type별 제목·본문 예시 모음이다. 메인 스킬 파일은 규칙만 담고, 구체 예시는 이 파일에서 관리한다.

## type별 제목 예시

| type | 라벨 | 제목 예시 |
|------|------|-----------|
| feat | `✨feat`, `BE` | `[feat] 친구 초대 알림 Redis Stream 발행 추가` |
| fix | `🐞bug`, `BE` | `[fix] 카드 점수 집계 누락 수정` |
| refactor | `🛠️refactor`, `BE` | `[refactor] Room-GameSession 책임 분리` |
| chore | `⚙️chore`, `BE` | `[chore] Testcontainers reuse 재활성화` |
| docs | `📝docs`, `BE` | `[docs] ADR-0020 작성` |
| test | `🧪 test`, `BE` | `[test] 카드 점수 집계 회귀 테스트 추가` |

## 본문 작성 예시

`.github/pull_request_template.md`의 4개 섹션을 그대로 유지하고 아래처럼 채운다.

```text
# ✅ 체크리스트

- [x] merge 타겟 브랜치 잘 설정되었는지 확인하기 (fe/dev, be/dev)

# 🔥 연관 이슈

- close #1404

# 🚀 작업 내용

1. 카드 점수 집계 시 마지막 라운드가 누락되던 인덱스 오류 수정
2. 누락 재현 테스트 추가

# 💬 리뷰 중점사항

- 집계 경계 조건(마지막 라운드 포함)에 대한 검증 로직을 중점적으로 확인 부탁드립니다.
```

연관 이슈가 없으면 `🔥 연관 이슈` 항목에 `없음`을 적는다.
