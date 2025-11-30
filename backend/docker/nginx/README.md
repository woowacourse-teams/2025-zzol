# Nginx Gateway

모든 외부 트래픽을 관리하는 리버스 프록시 게이트웨이입니다.

## 역할

Nginx는 모든 네트워크의 진입점으로:
- Dev 환경 (dev.zzol.site → dev-app)
- Prod 환경 (zzol.site → prod-app)
- Monitoring (status.zzol.site → grafana)

각 백엔드 서비스로 트래픽을 라우팅합니다.

## 도메인 매핑

| 도메인 | 백엔드 | 네트워크 |
|--------|--------|----------|
| dev.zzol.site | dev-app:8080 | dev-network |
| zzol.site | prod-app:8080 | prod-network |
| status.zzol.site | grafana:3000 | monitoring-network |

## 사용법

### 1. 실행

```bash
docker-compose up -d
```

### 2. 설정 변경 후 재로드

```bash
# 설정 파일 문법 체크
docker exec nginx nginx -t

# 재로드 (다운타임 없음)
docker exec nginx nginx -s reload

# 또는 컨테이너 재시작
docker-compose restart nginx
```

### 3. 로그 확인

```bash
# 전체 로그
docker-compose logs -f nginx

# Access 로그만
docker exec nginx tail -f /var/log/nginx/access.log

# Error 로그만
docker exec nginx tail -f /var/log/nginx/error.log

# 환경별 로그
docker exec nginx tail -f /var/log/nginx/dev-access.log
docker exec nginx tail -f /var/log/nginx/prod-access.log
```

## 설정 파일

- `conf/nginx.conf`: 메인 설정
- `conf/conf.d/default.conf`: 가상 호스트 및 프록시 설정

## SSL/TLS 설정

HTTPS를 활성화하려면:

1. SSL 인증서 파일을 `ssl/` 디렉토리에 배치:
   ```
   ssl/
   ├── fullchain.pem
   └── privkey.pem
   ```

2. `conf/conf.d/default.conf`에서 HTTPS 섹션 주석 해제

3. Nginx 재시작:
   ```bash
   docker-compose restart nginx
   ```

## 주의사항

- Nginx는 dev-network, prod-network, monitoring-network에 모두 연결됩니다
- 외부에서는 오직 Nginx를 통해서만 백엔드에 접근할 수 있습니다
- 설정 변경 시 반드시 `nginx -t`로 문법 검사 후 적용하세요
