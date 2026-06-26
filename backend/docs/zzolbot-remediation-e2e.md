# ZzolBot 자동 수정 봇 — 수동 e2e 검증 절차

자동 수정 봇(zzolbot 4편)의 파이프라인을 안전하게 점검하는 절차다. 운영자가 모니터링 알림에서
"수정 시도"를 누르면 앱이 결함을 분류·디스패치하고, GitHub Actions 워커(`zzolbot-remediation.yml`)가
결함 위치를 특정→Gemini로 수정·재현 테스트 제안→재현 테스트 RED→GREEN + 모듈 테스트 통과 시에만
`be/zzolbot/auto-fix-*` 브랜치로 PR을 연다. 봇은 PR 발행까지만 하며 머지 게이트는 사람 리뷰 + CI다.

## 사전 조건

- 워크플로우 `zzolbot-remediation.yml`이 **기본 브랜치에 존재**해야 한다(`repository_dispatch`·`workflow_dispatch`는 기본 브랜치 정의를 실행).
- GitHub Secrets: `ZZOL_BOT_GH_DISPATCH_TOKEN`(fine-grained PAT, contents·pull-requests write), `GEMINI_ZZOL_BOT_API_KEY`, (선택) `ZZOL_BOT_SLACK_WEBHOOK_URL`.
- 콜백까지 검증하려면: GitHub Secrets `ZZOL_BOT_APP_CALLBACK_URL`(앱 공개 베이스 URL)·`ZZOL_BOT_REMEDIATION_CALLBACK_TOKEN`, 그리고 앱 env에 같은 `ZZOL_BOT_REMEDIATION_CALLBACK_TOKEN`.
- 앱 트리거(어드민 버튼)까지 검증하려면: 앱 env `ZZOL_BOT_REMEDIATION_ENABLED=true` + `ZZOL_BOT_GH_DISPATCH_TOKEN`.

## 경로 1 — 음성 스모크 (PR 없이 게이트만 확인)

실제 버그 없이 "위치 특정 → 에이전트 → 게이트"가 도는지 확인한다. 재현되지 않는(정상 코드) 스택트레이스를 주면
재현 테스트가 RED가 되지 않아 `NO_FIX`로 떨어지고 **PR이 열리지 않아야** 한다.

1. GitHub → Actions → "ZzolBot Auto-Fix Remediation" → Run workflow.
2. 입력: `ref=be/dev`, `defectType=NULL_POINTER`, `stackTrace`에 be/dev에 실재하는 파일의 프레임을 넣는다(아래 예시 형식).
3. 기대 결과: 워크플로우 성공, 로그에 `NO_FIX`, 새 브랜치·PR 없음, Slack에 `NO_FIX` 통지.

스택트레이스 형식 예시:

```text
java.lang.NullPointerException: sample
	at coffeeshout.room.application.RoomService.find(RoomService.java:42)
```

## 경로 2 — 통제된 결함 양성 e2e (RED→GREEN→PR)

실제 운영 코드를 건드리지 않도록 **throwaway 브랜치에 자기완결적 샌드박스 결함**을 주입하고, 워크플로우의
`ref` 입력으로 그 브랜치를 타깃한다. PR base도 그 브랜치라 diff는 봇의 수정만 깔끔하게 남는다.

### 1. throwaway 브랜치에 샌드박스 결함 주입

```bash
git checkout -b be/test/zzolbot-e2e origin/be/dev
mkdir -p backend/app/src/main/java/coffeeshout/sandbox
cat > backend/app/src/main/java/coffeeshout/sandbox/RemediationE2eSandbox.java <<'EOF'
package coffeeshout.sandbox;

import java.util.Optional;

/** zzol-bot 자동 수정 e2e 검증용 샌드박스. 운영 코드에서 호출하지 않는다. */
public class RemediationE2eSandbox {

    public int lengthOf(String value) {
        // 의도된 결함: 빈 입력에 대한 가드 없이 역참조한다.
        final Optional<String> resolved = Optional.ofNullable(value);
        return resolved.get().length();
    }
}
EOF
git add backend/app/src/main/java/coffeeshout/sandbox/RemediationE2eSandbox.java
git commit -m "test: zzol-bot 자동 수정 e2e 샌드박스 결함 주입"
git push -u origin HEAD:be/test/zzolbot-e2e
```

### 2. 워크플로우 실행

GitHub → Actions → "ZzolBot Auto-Fix Remediation" → Run workflow. 입력:

- `ref`: `be/test/zzolbot-e2e`
- `defectType`: `NULL_POINTER`
- `rootCauseHypothesis`: `RemediationE2eSandbox.lengthOf 에서 빈 Optional.get() 역참조로 NPE`
- `stackTrace`:

```text
java.lang.NullPointerException
	at java.base/java.util.Optional.get(Optional.java:143)
	at coffeeshout.sandbox.RemediationE2eSandbox.lengthOf(RemediationE2eSandbox.java:11)
```

### 3. 기대 결과

| 단계 | 기대 |
| --- | --- |
| 위치 특정 | `backend/app/src/main/java/coffeeshout/sandbox/RemediationE2eSandbox.java`, module `:app` |
| 재현 테스트 | 수정 전 RED → 수정 후 GREEN |
| 게이트 | `:app:test` 통과(회귀 없음) |
| PR | `be/zzolbot/auto-fix-<run_id>` → base `be/test/zzolbot-e2e`, 본문에 근본원인·검증 증거·🤖 배너 |

### 4. 정리

```bash
gh pr close <PR_NUMBER> --delete-branch
git push origin --delete be/test/zzolbot-e2e
```

## 주의

- LLM은 비결정적이라 양성 e2e가 **항상 PR로 끝나는 것은 아니다**. `NO_FIX`(재현 테스트 컴파일 실패·결함 미재현 등)도 정상적 결과이며, 봇이 틀린 PR을 열지 않는다는 안전장치의 작동을 뜻한다.
- `attemptId`를 비우면 앱 콜백은 생략된다(어드민 상태 갱신 없음). 콜백까지 보려면 앱에서 디스패치된 실제 `attemptId`로 실행한다.
- 양성 e2e는 일회성이다. 같은 샌드박스로 다시 돌리려면 throwaway 브랜치를 재생성한다.
