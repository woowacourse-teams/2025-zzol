# Coffee Shout - Docker Configuration

Coffee Shout 백엔드 애플리케이션의 Docker 구성 파일들입니다.

## 📁 디렉토리 구조

```
docker/
├── app/
│   └── Dockerfile                   # Spring Boot 애플리케이션 이미지
│
├── dev/
│   ├── docker-compose.yml           # Dev 환경 (app + mysql + redis)
│   ├── .env.example                 # Dev 환경변수 예시
│   └── README.md                    # Dev 환경 가이드
│
├── prod/
│   ├── docker-compose.yml           # Prod 환경 (app + mysql + redis)
│   ├── .env.example                 # Prod 환경변수 예시
│   └── README.md                    # Prod 환경 가이드
│
├── monitoring/
│   ├── docker-compose.yml           # 모니터링 스택
│   ├── .env.example
│   ├── README.md
│   └── conf/
│       ├── prometheus.yml           # Prometheus 설정
│       ├── loki.yml                 # Loki 설정
│       └── tempo.yml                # Tempo 설정
│
├── nginx/
│   ├── docker-compose.yml           # Nginx 리버스 프록시
│   ├── README.md
│   └── conf/
│       ├── nginx.conf               # Nginx 메인 설정
│       └── conf.d/
│           └── default.conf         # 가상 호스트 설정
│
├── DEPLOYMENT.md                    # 📖 배포 가이드 (필독!)
└── README.md                        # 이 파일
```

## 🚀 빠른 시작

### 1. 로컬 개발 환경

```bash
# 환경변수 설정
cd backend/docker/dev
cp .env.example .env
# .env 파일 수정 (DB_PASSWORD, REGISTRY, IMAGE_TAG)

# JAR 빌드 (GitHub Actions에서 자동)
cd ../../
./gradlew bootJar

# Docker 이미지 빌드 및 GHCR에 푸시 (GitHub Actions에서 자동)
docker build -t ghcr.io/{owner}/coffee-shout-backend:dev -f docker/app/Dockerfile build/libs/
docker push ghcr.io/{owner}/coffee-shout-backend:dev

# 또는 로컬에서 테스트
docker build -t coffee-shout-backend:local -f docker/app/Dockerfile build/libs/

# 실행 (GHCR에서 이미지 pull)
cd docker/dev
docker-compose up -d

# 로그 확인
docker-compose logs -f
```

### 2. 운영 서버 배포

**✅ 자동 배포 (GitHub Actions) - 권장**

```bash
# Dev 배포 (be/dev 브랜치)
git push origin be/dev

# Prod 배포 (be/prod 브랜치)
git push origin be/prod
```

**배포 프로세스**:
1. GitHub Actions에서 JAR 빌드
2. Docker 이미지 빌드 및 **GHCR**(GitHub Container Registry)에 푸시
3. 서버에서 GHCR로부터 이미지 pull
4. docker-compose로 컨테이너 실행

**수동 배포**

자세한 내용은 [DEPLOYMENT.md](./DEPLOYMENT.md)를 참고하세요.

## 🌐 네트워크 구성

각 환경은 독립된 네트워크로 격리되어 있습니다:

- `dev-network`: Dev 환경 (dev-app, dev-mysql, dev-redis)
- `prod-network`: Prod 환경 (prod-app, prod-mysql, prod-redis)
- `monitoring-network`: 모니터링 스택

**Nginx**는 모든 네트워크에 연결되어 게이트웨이 역할을 합니다.

## 📊 포트 매핑

### Dev 환경
- Dev App: 8080
- Dev MySQL: 3307 (호스트) → 3306 (컨테이너)
- Dev Redis: 6380 (호스트) → 6379 (컨테이너)

### Prod 환경
- Prod App: 8081
- Prod MySQL: 3308 (호스트) → 3306 (컨테이너)
- Prod Redis: 6381 (호스트) → 6379 (컨테이너)

### Nginx
- HTTP: 80
- HTTPS: 443

### Monitoring
- Grafana: 3000 (Nginx를 통해 접근)
- Prometheus: 9090 (내부)
- Loki: 3100 (내부)
- Tempo: 3200 (내부)

## 🔧 주요 명령어

### 개발 환경

```bash
# 시작
cd docker/dev
docker-compose up -d

# 중지
docker-compose down

# App만 재시작
docker-compose restart dev-app

# 로그 확인
docker-compose logs -f dev-app
```

### 운영 환경

```bash
# 시작
cd docker/prod
docker-compose up -d

# App만 재배포 (DB 유지)
docker-compose up -d --no-deps prod-app

# 전체 중지
docker-compose down
```

## 📖 상세 문서

- **[DEPLOYMENT.md](./DEPLOYMENT.md)**: 배포 가이드 (필독!)
- **[dev/README.md](./dev/README.md)**: Dev 환경 상세 가이드
- **[prod/README.md](./prod/README.md)**: Prod 환경 상세 가이드
- **[monitoring/README.md](./monitoring/README.md)**: 모니터링 스택 가이드
- **[nginx/README.md](./nginx/README.md)**: Nginx 설정 가이드

## 🔐 보안 주의사항

1. **환경변수 파일 (.env)**
   - `.env` 파일은 Git에 커밋하지 마세요
   - `.env.example`을 복사하여 사용하세요
   - GitHub Secrets를 사용하여 자동 생성됩니다

2. **SSL/TLS 인증서**
   - `nginx/ssl/` 디렉토리에 인증서 배치
   - 인증서 파일도 Git에 커밋하지 마세요

3. **DB 비밀번호**
   - 강력한 비밀번호 사용
   - 정기적으로 변경
   - GitHub Secrets로 관리

## 🐛 트러블슈팅

문제가 발생하면 [DEPLOYMENT.md](./DEPLOYMENT.md)의 트러블슈팅 섹션을 참고하세요.

### 자주 발생하는 문제

1. **컨테이너가 시작되지 않음**
   ```bash
   docker-compose logs <service-name>
   ```

2. **포트 충돌**
   ```bash
   sudo netstat -tulpn | grep :<port>
   ```

3. **네트워크 문제**
   ```bash
   docker network inspect dev-network
   ```

## 📞 지원

이슈가 있으면 GitHub Issues에 등록해주세요.
