# Dev Environment

개발 환경용 Docker Compose 구성입니다.

## 구성

- **dev-app**: Spring Boot 애플리케이션 (포트: 8080)
- **dev-mysql**: MySQL 8.0 데이터베이스 (포트: 3307)
- **dev-redis**: Valkey (Redis 호환) (포트: 6380)

## 사용법

### 1. 환경변수 설정

```bash
cp .env.example .env
# .env 파일을 수정하여 실제 값 입력
```

### 2. 실행

```bash
# 전체 스택 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스만 실행
docker-compose up -d dev-mysql dev-redis
```

### 3. 중지

```bash
# 중지
docker-compose down

# 중지 + 볼륨 삭제 (데이터 초기화)
docker-compose down -v
```

## 주의사항

- `.env` 파일은 Git에 커밋하지 마세요
- GitHub Actions에서 배포 시 환경변수는 Secrets에서 주입됩니다
