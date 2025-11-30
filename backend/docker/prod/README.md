# Production Environment

운영 환경용 Docker Compose 구성입니다.

## 구성

- **prod-app**: Spring Boot 애플리케이션 (포트: 8081)
- **prod-mysql**: MySQL 8.0 데이터베이스 (포트: 3308)
- **prod-redis**: Valkey (Redis 호환) (포트: 6381)

## Dev와의 차이점

- 포트 번호가 다름 (같은 인스턴스에서 동시 실행 가능)
- 리소스 제한 설정
- MySQL 성능 튜닝 옵션 추가
- Redis 메모리 정책 설정
- 더 엄격한 Health Check

## 사용법

### 1. 환경변수 설정

```bash
cp .env.example .env
# .env 파일을 수정하여 실제 운영 값 입력
```

### 2. 실행

```bash
# 전체 스택 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스만 실행
docker-compose up -d prod-mysql prod-redis
```

### 3. 중지

```bash
# 중지 (데이터 유지)
docker-compose down

# 중지 + 볼륨 삭제 (⚠️ 주의: 데이터 삭제됨)
docker-compose down -v
```

## 백업

MySQL 데이터는 `prod-mysql-backup` 볼륨에 백업할 수 있습니다:

```bash
docker exec prod-mysql mysqldump -u root -p coffee_shout > backup.sql
```

## 주의사항

- 운영 환경이므로 변경 시 주의가 필요합니다
- `.env` 파일은 Git에 커밋하지 마세요
- GitHub Actions에서 배포 시 환경변수는 Secrets에서 주입됩니다
