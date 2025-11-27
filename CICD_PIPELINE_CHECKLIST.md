# CI/CD 파이프라인 구축 체크리스트

## 목표
특정 브랜치에 코드 머지 시 자동 배포
- GitHub Actions에서 JAR 빌드
- SCP로 파일 전송
- 서버에서 Docker Compose(Profiles)로 실행

---

## 1. GitHub Actions 워크플로우 설정

### 1.1 트리거 설정
- [ ] 배포 대상 브랜치 결정 (예: `be/dev`, `be/prod`)
- [ ] `push` 이벤트로 트리거 설정
- [ ] 필요시 `workflow_dispatch`로 수동 트리거 추가

### 1.2 빌드 환경 설정
- [ ] JDK 21 설정 (Corretto)
- [ ] Gradle 캐시 설정
- [ ] 빌드 working directory 설정 (`./backend`)

### 1.3 JAR 빌드
- [ ] Gradle 실행 권한 부여 (`chmod +x gradlew`)
- [ ] 테스트 실행 (`./gradlew test`)
- [ ] JAR 빌드 (`./gradlew bootJar` 또는 `./gradlew build`)
- [ ] 빌드된 JAR 파일 경로 확인 (`backend/build/libs/*.jar`)
- [ ] JAR 파일명 규칙 정의 (예: `app-{version}.jar` 또는 `app.jar`)

---

## 2. GitHub Secrets 설정

### 2.1 SSH 접속 정보
- [ ] `SSH_HOST` - 배포 서버 IP/도메인
- [ ] `SSH_PORT` - SSH 포트 (기본: 22)
- [ ] `SSH_USERNAME` - SSH 사용자명
- [ ] `SSH_PRIVATE_KEY` - SSH Private Key
- [ ] `SSH_KNOWN_HOSTS` - (선택) Known hosts 정보

### 2.2 배포 환경별 Secrets (필요시)
- [ ] `DEV_SERVER_HOST` - 개발 서버
- [ ] `PROD_SERVER_HOST` - 운영 서버
- [ ] Docker Compose 환경변수 (DB 정보 등)

---

## 3. SCP 파일 전송 설정

### 3.1 전송 준비
- [ ] 서버 디렉토리 구조 설계
  - 예: `/home/deploy/app/jar/`
  - 예: `/home/deploy/app/docker-compose.yml`
- [ ] SSH 키 기반 인증 설정
- [ ] 서버 사용자 권한 확인

### 3.2 SCP 전송 구현
- [ ] `appleboy/scp-action` 또는 직접 `scp` 명령어 사용 결정
- [ ] JAR 파일 전송 경로 설정
- [ ] Docker Compose 파일 전송 (필요시)
- [ ] 환경 설정 파일 전송 (`.env` 등)
- [ ] 전송 실패 시 재시도 로직

---

## 4. 서버 환경 설정

### 4.1 Docker 설치
- [ ] Docker Engine 설치
- [ ] Docker Compose 설치 (v2 권장)
- [ ] Docker 서비스 자동 시작 설정

### 4.2 디렉토리 구조
```
/home/deploy/app/
├── docker-compose.yml
├── jar/
│   └── app.jar
├── logs/
└── .env (환경변수)
```
- [ ] 필요한 디렉토리 생성
- [ ] 적절한 권한 설정

### 4.3 Docker Compose 파일 작성
- [ ] `docker-compose.yml` 작성
- [ ] Profiles 설정 (`dev`, `prod` 등)
- [ ] Volume 매핑 (JAR, 로그 등)
- [ ] 네트워크 설정
- [ ] 환경변수 설정
- [ ] 헬스체크 설정

예시:
```yaml
services:
  app:
    image: openjdk:21-jdk-slim
    profiles: ["dev"]
    volumes:
      - ./jar/app.jar:/app/app.jar
      - ./logs:/app/logs
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    command: java -jar /app/app.jar

  app-prod:
    image: openjdk:21-jdk-slim
    profiles: ["prod"]
    volumes:
      - ./jar/app.jar:/app/app.jar
      - ./logs:/app/logs
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    command: java -jar /app/app.jar
```

---

## 5. SSH 원격 실행 설정

### 5.1 배포 스크립트 작성
- [ ] 서버에 배포 스크립트 작성 (`deploy.sh`)
  - 기존 컨테이너 중지
  - 새 JAR로 교체 확인
  - Docker Compose로 새 컨테이너 시작
  - 헬스체크 확인
  - 롤백 로직 (선택)

### 5.2 SSH 명령 실행
- [ ] `appleboy/ssh-action` 사용 결정
- [ ] 실행할 명령어 정의
  ```bash
  cd /home/deploy/app
  docker compose --profile dev down
  docker compose --profile dev up -d
  docker compose --profile dev ps
  ```
- [ ] 실행 결과 로깅

---

## 6. 브랜치별 프로필 매핑

### 6.1 환경 분리
- [ ] `be/dev` → `dev` profile
- [ ] `be/prod` → `prod` profile
- [ ] GitHub Actions에서 브랜치명으로 프로필 자동 결정

### 6.2 조건부 실행
```yaml
- name: Set Profile
  run: |
    if [[ "${{ github.ref }}" == "refs/heads/be/dev" ]]; then
      echo "PROFILE=dev" >> $GITHUB_ENV
    elif [[ "${{ github.ref }}" == "refs/heads/be/prod" ]]; then
      echo "PROFILE=prod" >> $GITHUB_ENV
    fi
```
- [ ] 브랜치별 프로필 환경변수 설정
- [ ] 프로필에 맞는 서버로 배포

---

## 7. 모니터링 및 알림

### 7.1 배포 상태 확인
- [ ] Docker 컨테이너 상태 확인 (`docker compose ps`)
- [ ] 애플리케이션 헬스체크 엔드포인트 호출
- [ ] 로그 확인 (`docker compose logs`)

### 7.2 알림 설정 (선택)
- [ ] Slack/Discord 웹훅 설정
- [ ] 배포 성공/실패 알림
- [ ] GitHub Actions 요약에 배포 정보 출력

---

## 8. 보안 고려사항

### 8.1 SSH 키 관리
- [ ] Private Key는 GitHub Secrets에만 저장
- [ ] 서버에 Public Key 등록
- [ ] 키 파일 권한 적절히 설정 (600)

### 8.2 환경변수 관리
- [ ] 민감한 정보는 GitHub Secrets 사용
- [ ] 서버의 `.env` 파일 권한 제한
- [ ] 로그에 민감 정보 노출 방지

### 8.3 네트워크 보안
- [ ] SSH 포트 변경 고려
- [ ] 방화벽 설정 (특정 IP만 SSH 허용)
- [ ] Fail2ban 등 brute-force 방어

---

## 9. 테스트 및 검증

### 9.1 로컬 테스트
- [ ] Docker Compose 파일 로컬 테스트
- [ ] 프로필별 동작 확인
- [ ] JAR 파일 정상 실행 확인

### 9.2 스테이징 배포
- [ ] 개발 환경에 먼저 배포 테스트
- [ ] 롤백 시나리오 테스트
- [ ] 배포 시간 측정

### 9.3 프로덕션 배포
- [ ] 배포 전 체크리스트 확인
- [ ] 모니터링 준비
- [ ] 롤백 계획 수립

---

## 10. 문서화 및 유지보수

### 10.1 문서 작성
- [ ] 배포 프로세스 README 작성
- [ ] 트러블슈팅 가이드 작성
- [ ] 서버 접속 정보 문서화 (보안 고려)

### 10.2 CI/CD 개선
- [ ] 배포 시간 최적화
- [ ] 캐시 전략 개선
- [ ] 무중단 배포 고려 (Blue-Green, Rolling)

---

## 참고: 예상 워크플로우 구조

```yaml
name: Backend Deploy

on:
  push:
    branches:
      - be/dev
      - be/prod
    paths:
      - "backend/**"

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
      - name: Setup JDK
      - name: Build JAR
      - name: Set Profile from Branch
      - name: SCP Transfer
      - name: SSH Execute Docker Compose
      - name: Health Check
      - name: Notify Result
```

---

## 체크포인트
- [ ] 전체 파이프라인이 한 번에 성공적으로 실행되는가?
- [ ] 각 브랜치별로 올바른 환경에 배포되는가?
- [ ] 배포 실패 시 적절히 알림이 오는가?
- [ ] 롤백이 가능한가?
- [ ] 보안 취약점은 없는가?
