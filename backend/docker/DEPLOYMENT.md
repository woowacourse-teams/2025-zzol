# Coffee Shout - Docker 배포 가이드

이 문서는 Coffee Shout 백엔드 애플리케이션의 Docker 기반 배포 가이드입니다.

## 📋 목차

1. [아키텍처 개요](#아키텍처-개요)
2. [사전 준비](#사전-준비)
3. [GitHub Secrets 설정](#github-secrets-설정)
4. [자동 배포 (GitHub Actions)](#자동-배포-github-actions)
5. [수동 배포](#수동-배포)
6. [모니터링 및 Nginx 설정](#모니터링-및-nginx-설정)
7. [트러블슈팅](#트러블슈팅)

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
│   ├── .env
│   └── app.jar
├── prod/
│   ├── docker-compose.yml
│   ├── .env
│   └── app.jar
├── monitoring/
│   ├── docker-compose.yml
│   ├── .env
│   └── conf/
├── nginx/
│   ├── docker-compose.yml
│   └── conf/
└── common/
    └── Dockerfile
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
# be/dev 브랜치에 푸시
git checkout be/dev
git push origin be/dev
```

**배포 프로세스:**
1. JAR 빌드
2. 파일 전송 (JAR, docker-compose.yml, Dockerfile)
3. 네트워크 생성
4. .env 파일 생성
5. Docker 이미지 빌드
6. dev-app 컨테이너만 재시작 (DB는 유지)
7. Health Check

### Prod 환경 배포

```bash
# be/prod 브랜치에 푸시
git checkout be/prod
git push origin be/prod
```

### 수동 트리거

GitHub Actions 탭에서 `Backend Deploy` 워크플로우를 선택하고 "Run workflow"로 수동 실행 가능.

---

## 수동 배포

### Dev 환경 수동 배포

```bash
# 1. JAR 빌드 (로컬)
./gradlew bootJar

# 2. 서버로 파일 전송
scp backend/build/libs/*.jar user@host:~/dev/app.jar
scp backend/docker/dev/docker-compose.yml user@host:~/dev/
scp backend/docker/app/Dockerfile user@host:~/common/

# 3. 서버에서 배포
ssh user@host
cd ~/dev

# .env 파일 생성
cat > .env << EOF
DB_PASSWORD=your-password
EOF

# Docker 이미지 빌드
docker build -t coffee-shout-backend:dev -f ~/common/Dockerfile .

# 컨테이너 배포
docker-compose up -d

# 또는 App만 재시작
docker-compose up -d --no-deps dev-app
```

### Prod 환경 수동 배포

위와 동일하되 `~/dev`를 `~/prod`로 변경.

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
