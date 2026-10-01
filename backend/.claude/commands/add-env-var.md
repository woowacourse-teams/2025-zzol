추가할 환경변수명을 인자로 받아 세 파일에 자동으로 삽입한다: `$ARGUMENTS`

## 작업 순서

다음 세 파일을 **동시에** 수정한다.

### 1. `backend/docker/dev/docker-compose.yml`

`x-dev-app.environment` 블록 끝에 추가:

```yaml
    - <VARIABLE_NAME>=${<VARIABLE_NAME>}
```

### 2. `backend/docker/prod/docker-compose.yml`

`x-prod-app.environment` 블록 끝에 추가:

```yaml
    - <VARIABLE_NAME>=${<VARIABLE_NAME>}
```

### 3. `.github/workflows/backend-cd.yml`

`Create .env file` 스텝의 heredoc 끝(`EOF` 바로 위)에 추가:

```text
          <VARIABLE_NAME>=${{ secrets.<VARIABLE_NAME> }}
```

`<VARIABLE_NAME>`은 `$ARGUMENTS`로 바꾼다. 세 파일에 같은 이름이 이미 있으면 추가하지 않고 알린다.

## 완료 후 출력

수정 완료 후 아래 메시지를 출력한다:

---

**GitHub Secrets 등록 필요**
GitHub Repository → Settings → Secrets and variables → Actions 에서 `<VARIABLE_NAME>` 값을 등록해야 CD가 정상 동작합니다. (예: `ROOM_SESSION_TOKEN_SECRET`)
