# Coffee Shout - Docker 배포 가이드 (GHCR)

이 문서는 Coffee Shout 백엔드 애플리케이션의 **GHCR(GitHub Container Registry) 기반** Docker 배포 가이드입니다.

## 📋 목차

1. [아키텍처 개요](#아키텍처-개요)
2. [GHCR 배포 방식](#ghcr-배포-방식)
3. [사전 준비](#사전-준비)
4. [GitHub Secrets 설정](#github-secrets-설정)
5. [자동 배포 (GitHub Actions)](#자동-배포-github-actions)
6. [수동 배포](#수동-배포)
7. [모니터링 및 Nginx 설정](#모니터링-및-nginx-설정)
8. [트러블슈팅](#트러블슈팅)

---

## 아키텍처 개요

### 네트워크 구조

```
┌─────────────────────────────────────────────────────────┐
│                    Oracle Cloud Instance                 │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ dev-network  │  │ prod-network │  │monitoring-net│  │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤  │
│  │ dev-app      │  │ prod-app     │  │ prometheus   │  │
│  │ dev-mysql    │  │ prod-mysql   │  │ grafana      │  │
│  │ dev-redis    │  │ prod-redis   │  │ loki         │  │
│  └──────────────┘  └──────────────┘  │ tempo        │  │
│                                       │ cadvisor     │  │
│         ┌──────────────────────────┐  └──────────────┘  │
│         │       nginx              │                     │
│         │  (모든 네트워크 연결)    │                     │
│         └──────────────────────────┘                     │
│                     │                                     │
└─────────────────────┼─────────────────────────────────────┘
                      │
                  [외부 접속]
```

### 디렉토리 구조

**서버 (Oracle 인스턴스):**
```
~/ (홈 디렉토리)
├── dev/
│   ├── docker-compose.yml
│   └── .env                        # DB_PASSWORD, REGISTRY, IMAGE_TAG
├── prod/
│   ├── docker-compose.yml
│   └── .env                        # DB_PASSWORD, REGISTRY, IMAGE_TAG
├── monitoring/
│   ├── docker-compose.yml
│   ├── .env
│   └── conf/
└── nginx/
    ├── docker-compose.yml
    └── conf/
```

**주요 변경사항**:
- ✅ **JAR 파일 불필요** (GHCR에서 이미지 pull)
- ✅ **Dockerfile 불필요** (GitHub Actions에서 빌드)
- ✅ **디스크 사용량 감소**

---

## GHCR 배포 방식

### 왜 GHCR을 사용하나요?

**기존 방식 (서버에서 빌드)**:
```
GitHub Actions (무료)          EC2 인스턴스 (유료)
     ↓                              ↓
JAR 빌드                       Docker 빌드 ← CPU/메모리 사용!
     ↓                              ↓
JAR + Dockerfile 전송           이미지 생성
                                   ↓
                              컨테이너 실행
```

**GHCR 방식 (GitHub Actions에서 빌드)**:
```
GitHub Actions (무료)          EC2 인스턴스 (유료)
     ↓                              ↓
JAR 빌드                       이미지 Pull ← 네트워크만 사용!
     ↓                              ↓
Docker 이미지 빌드              컨테이너 실행
     ↓
GHCR에 Push
```

### 장점

| 항목 | 기존 방식 | GHCR 방식 |
|------|----------|-----------|
| **EC2 CPU 사용** | 빌드 시마다 사용 | 거의 없음 (pull만) |
| **EC2 메모리** | 빌드 중 spike | 안정적 |
| **빌드 속도** | 서버 성능에 의존 | GitHub Actions 캐시 활용 |
| **이미지 관리** | 로컬만 | 버전 관리 가능 |
| **롤백** | 어려움 | 이전 이미지로 즉시 롤백 |
| **비용** | EC2 리소스 사용 | GitHub 무료 리소스 활용 |

### GHCR 이미지 구조

```
ghcr.io/woowacourse-teams/coffee-shout-backend
├── :dev                    # Dev 환경 최신 이미지
├── :dev-{commit-sha}       # Dev 환경 특정 커밋 이미지
├── :prod                   # Prod 환경 최신 이미지
└── :prod-{commit-sha}      # Prod 환경 특정 커밋 이미지
```

**예시**:
```
ghcr.io/woowacourse-teams/coffee-shout-backend:dev
ghcr.io/woowacourse-teams/coffee-shout-backend:dev-d66065ac
ghcr.io/woowacourse-teams/coffee-shout-backend:prod
ghcr.io/woowacourse-teams/coffee-shout-backend:prod-ae72d781
```

### 배포 플로우

```
1. 코드 Push (be/dev 또는 be/prod)
   ↓
2. GitHub Actions 트리거
   ↓
3. JAR 빌드 (./gradlew bootJar)
   ↓
4. Docker 이미지 빌드
   - context: backend/build/libs
   - file: backend/docker/app/Dockerfile
   ↓
5. GHCR에 Push
   - ghcr.io/.../coffee-shout-backend:dev
   - ghcr.io/.../coffee-shout-backend:dev-{sha}
   ↓
6. docker-compose.yml 전송 (SCP)
   ↓
7. 서버에서 작업
   ↓
   7-1. .env 파일 생성
        - DB_PASSWORD
        - REGISTRY=ghcr.io/woowacourse-teams
        - IMAGE_TAG=dev
   ↓
   7-2. GHCR 로그인
        echo $GITHUB_TOKEN | docker login ghcr.io
   ↓
   7-3. 이미지 Pull
        docker pull ghcr.io/.../coffee-shout-backend:dev
   ↓
   7-4. 컨테이너 실행
        docker-compose up -d --no-deps dev-app
   ↓
8. Health Check
   - /actuator/health 확인 (40초)
```

---

## 사전 준비

### 1. 서버 준비

Oracle Cloud 인스턴스에 다음을 설치:

```bash
# Docker 설치
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. 네트워크 생성

```bash
docker network create dev-network
docker network create prod-network
docker network create monitoring-network
```

---

## GitHub Secrets 설정

GitHub Repository → Settings → Secrets and variables → Actions에서 다음 Secrets 추가:

### 필수 Secrets

| Secret 이름 | 설명 | 예시 |
|-------------|------|------|
| `SSH_HOST` | Oracle 인스턴스 IP | `123.45.67.89` |
| `SSH_USERNAME` | SSH 사용자명 | `ubuntu` |
| `SSH_PRIVATE_KEY` | SSH Private Key | `-----BEGIN RSA PRIVATE KEY-----...` |
| `SSH_PORT` | SSH 포트 | `22` |
| `DB_PASSWORD` | MySQL Root 비밀번호 | `your-secure-password` |

### 선택적 Secrets (모니터링)

| Secret 이름 | 설명 |
|-------------|------|
| `GRAFANA_ADMIN_PASSWORD` | Grafana 관리자 비밀번호 |
| `OCI_ACCESS_KEY` | OCI Object Storage Access Key |
| `OCI_SECRET_KEY` | OCI Object Storage Secret Key |

---

## 자동 배포 (GitHub Actions)

### Dev 환경 배포

```bash
# be/dev 브랜치에 푸시 (PR merge 또는 직접 push)
git checkout be/dev
git push origin be/dev
```

**배포 프로세스 (GHCR 방식)**:
1. ✅ **JAR 빌드** (GitHub Actions)
   - `./gradlew bootJar`
2. ✅ **Docker 이미지 빌드** (GitHub Actions)
   - GHCR 로그인
   - Docker Buildx 설정
   - 이미지 빌드 (캐시 활용)
3. ✅ **GHCR에 푸시**
   - `ghcr.io/woowacourse-teams/coffee-shout-backend:dev`
   - `ghcr.io/woowacourse-teams/coffee-shout-backend:dev-{sha}`
4. ✅ **파일 전송** (SCP)
   - `docker-compose.yml` 만 전송
5. ✅ **서버에서 배포**
   - 네트워크 생성
   - `.env` 파일 생성 (REGISTRY, IMAGE_TAG 포함)
   - GHCR 로그인
   - **이미지 Pull** (빌드 불필요!)
   - 컨테이너 실행 (`docker-compose up -d --no-deps dev-app`)
6. ✅ **Health Check**
   - 40초간 `/actuator/health` 확인

**리소스 사용**:
- GitHub Actions: JAR 빌드 + Docker 빌드 (무료)
- EC2 인스턴스: 이미지 Pull + 컨테이너 실행 (최소화)

### Prod 환경 배포

```bash
# be/prod 브랜치에 푸시
git checkout be/prod
git push origin be/prod
```

배포 프로세스는 Dev와 동일하며, 환경만 `prod`로 변경됩니다.

### 수동 트리거

GitHub Actions 탭에서 `Backend Deploy` 워크플로우를 선택하고 "Run workflow"로 수동 실행 가능.

**중요**: 브랜치와 환경이 일치해야 합니다!
- ✅ `be/dev` 브랜치 + `dev` 환경 선택
- ✅ `be/prod` 브랜치 + `prod` 환경 선택
- ❌ `be/dev` 브랜치 + `prod` 환경 선택 → 에러 발생!

---

## 수동 배포

### 방법 1: GHCR에서 Pull (추천)

```bash
# 1. 서버 접속
ssh user@host
cd ~/dev

# 2. .env 파일 생성
cat > .env << EOF
DB_PASSWORD=your-password
REGISTRY=ghcr.io/woowacourse-teams
IMAGE_TAG=dev
EOF

# 3. docker-compose.yml 업데이트 (필요시)
# SCP로 전송하거나 직접 수정

# 4. GHCR 로그인
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 5. 최신 이미지 Pull
docker pull ghcr.io/woowacourse-teams/coffee-shout-backend:dev

# 6. 컨테이너 재시작
docker-compose up -d --no-deps dev-app

# 7. Health Check
docker-compose logs -f dev-app
```

### 방법 2: 특정 커밋 버전으로 롤백

```bash
# 1. 서버 접속
ssh user@host
cd ~/dev

# 2. 이전 커밋 이미지 Pull
docker pull ghcr.io/woowacourse-teams/coffee-shout-backend:dev-d66065ac

# 3. .env에서 IMAGE_TAG 변경
cat > .env << EOF
DB_PASSWORD=your-password
REGISTRY=ghcr.io/woowacourse-teams
IMAGE_TAG=dev-d66065ac
EOF

# 4. 컨테이너 재시작
docker-compose up -d --no-deps dev-app
```

### 방법 3: 로컬에서 빌드 및 배포 (비상시)

```bash
# 1. JAR 빌드 (로컬)
./gradlew bootJar

# 2. Docker 이미지 빌드 및 GHCR 푸시 (로컬)
docker build -t ghcr.io/woowacourse-teams/coffee-shout-backend:dev-manual \
  -f backend/docker/app/Dockerfile backend/build/libs/

# GHCR 로그인
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 이미지 푸시
docker push ghcr.io/woowacourse-teams/coffee-shout-backend:dev-manual

# 3. 서버에서 Pull 및 배포 (방법 1 참조)
```

### Prod 환경 수동 배포

위와 동일하되 다음을 변경:
- `~/dev` → `~/prod`
- `IMAGE_TAG=dev` → `IMAGE_TAG=prod`
- `:dev` → `:prod`

---

## 모니터링 및 Nginx 설정

### 모니터링 스택 배포 (1회 실행)

```bash
ssh user@host

# 모니터링 파일 전송
cd ~/monitoring

# .env 파일 생성
cat > .env << EOF
GRAFANA_ADMIN_PASSWORD=your-password
OCI_ACCESS_KEY=your-key
OCI_SECRET_KEY=your-secret
EOF

# 설정 파일 복사
cp backend/docker/monitoring/conf/* ~/monitoring/conf/

# 배포
docker-compose up -d

# 로그 확인
docker-compose logs -f
```

### Nginx 배포 (1회 실행)

```bash
# Nginx 설정 파일 전송
cd ~/nginx
cp backend/docker/nginx/conf/nginx.conf ~/nginx/conf/
cp backend/docker/nginx/conf/conf.d/default.conf ~/nginx/conf/conf.d/

# 배포
docker-compose up -d

# 설정 변경 시 재로드
docker exec nginx nginx -t
docker exec nginx nginx -s reload
```

---

## 운영 명령어

### 컨테이너 관리

```bash
# 전체 스택 시작
cd ~/dev  # or ~/prod
docker-compose up -d

# 전체 스택 중지
docker-compose down

# App만 재시작 (DB 유지)
docker-compose restart dev-app

# App만 재배포 (이미지 재빌드)
docker-compose up -d --no-deps --build dev-app
```

### 로그 확인

```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f dev-app

# 최근 100줄
docker-compose logs --tail=100 dev-app
```

### 상태 확인

```bash
# 컨테이너 상태
docker-compose ps

# 리소스 사용량
docker stats

# Health check
docker exec dev-app wget --spider http://localhost:8080/actuator/health
```

---

## 트러블슈팅

### 1. 컨테이너가 시작되지 않음

```bash
# 로그 확인
docker-compose logs dev-app

# 이미지 재빌드
docker-compose build --no-cache dev-app
docker-compose up -d dev-app
```

### 2. DB 연결 실패

```bash
# MySQL 상태 확인
docker-compose logs dev-mysql

# MySQL 재시작
docker-compose restart dev-mysql

# 네트워크 확인
docker network inspect dev-network
```

### 3. Health Check 실패

```bash
# 컨테이너 내부 확인
docker exec -it dev-app sh
wget http://localhost:8080/actuator/health

# 애플리케이션 로그 확인
docker-compose logs --tail=200 dev-app
```

### 4. 포트 충돌

```bash
# 포트 사용 확인
sudo netstat -tulpn | grep :8080

# 충돌하는 컨테이너 중지
docker ps
docker stop <container_id>
```

### 5. 디스크 공간 부족

```bash
# 사용하지 않는 이미지/컨테이너 정리
docker system prune -a

# 볼륨 정리 (주의: 데이터 삭제됨)
docker volume prune
```

### 6. GHCR 이미지 Pull 실패

```bash
# 에러: denied: permission_denied
# 원인: GHCR 로그인 필요

# 해결 1: GitHub Token으로 로그인
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 해결 2: GitHub Actions에서 배포 재실행
# (자동으로 GITHUB_TOKEN 사용)

# 로그인 확인
docker info | grep Username
```

### 7. 이미지 태그 불일치

```bash
# 에러: image not found: ghcr.io/.../coffee-shout-backend:dev
# 원인: .env의 IMAGE_TAG와 실제 이미지 태그 불일치

# 확인: 사용 가능한 이미지 확인
docker images | grep coffee-shout-backend

# 또는 GHCR에서 확인
# https://github.com/orgs/woowacourse-teams/packages/container/coffee-shout-backend/versions

# 해결: .env 파일에서 IMAGE_TAG 수정
cat .env  # 현재 설정 확인
vi .env   # 수정
```

### 8. 브랜치-환경 불일치 에러 (workflow_dispatch)

```bash
# 에러: "prod 환경은 be/prod 브랜치에서만 배포 가능합니다!"
# 원인: 수동 실행 시 브랜치와 환경이 일치하지 않음

# 해결: 올바른 브랜치 선택
# - dev 배포: be/dev 브랜치 선택 + dev 환경 선택
# - prod 배포: be/prod 브랜치 선택 + prod 환경 선택
```

### 9. 이전 버전으로 롤백

```bash
# 1. GitHub에서 커밋 SHA 확인
# https://github.com/woowacourse-teams/2025-coffee-shout/commits/be/dev

# 2. 해당 커밋의 이미지 Pull
docker pull ghcr.io/woowacourse-teams/coffee-shout-backend:dev-{commit-sha}

# 3. .env 업데이트
cat > .env << EOF
DB_PASSWORD=your-password
REGISTRY=ghcr.io/woowacourse-teams
IMAGE_TAG=dev-{commit-sha}
EOF

# 4. 컨테이너 재시작
docker-compose up -d --no-deps dev-app

# 5. 확인
docker-compose logs -f dev-app
```

---

## 백업 및 복구

### MySQL 백업

```bash
# 백업
docker exec dev-mysql mysqldump -u root -p coffee_shout > backup_$(date +%Y%m%d).sql

# 복구
cat backup_20250101.sql | docker exec -i dev-mysql mysql -u root -p coffee_shout
```

### 전체 볼륨 백업

```bash
# 볼륨 목록 확인
docker volume ls

# 볼륨 백업
docker run --rm -v dev-mysql-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-data.tar.gz -C /data .

# 볼륨 복구
docker run --rm -v dev-mysql-data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql-data.tar.gz -C /data
```

---

## 참고 자료

- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Spring Boot Docker 가이드](https://spring.io/guides/topicals/spring-boot-docker/)
- [Prometheus 설정](https://prometheus.io/docs/prometheus/latest/configuration/configuration/)
- [Grafana 설정](https://grafana.com/docs/grafana/latest/)
