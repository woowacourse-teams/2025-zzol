# Monitoring Stack

Coffee Shout 모니터링 스택입니다.

## 구성

- **Prometheus**: 메트릭 수집 및 저장
- **Grafana**: 시각화 대시보드 (포트: 3000, status.zzol.site)
- **Loki**: 로그 수집 및 분석
- **Tempo**: 분산 트레이싱
- **cAdvisor**: 컨테이너 리소스 모니터링

## 네트워크 구성

모니터링 스택은 다음 네트워크에 연결됩니다:
- `monitoring-network`: 모니터링 서비스 간 통신
- `dev-network`: Dev 애플리케이션 메트릭 수집
- `prod-network`: Prod 애플리케이션 메트릭 수집

## 사용법

### 1. 환경변수 설정

```bash
cp .env.example .env
# .env 파일 수정
```

### 2. 실행

```bash
# 모니터링 스택 전체 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# Grafana만 재시작
docker-compose restart grafana
```

### 3. 접속

- Grafana: http://status.zzol.site (또는 http://localhost:3000)
  - ID: admin
  - PW: .env 파일의 GRAFANA_ADMIN_PASSWORD

### 4. 데이터 소스 설정

Grafana에서 다음 데이터 소스를 추가하세요:

**Prometheus**
- URL: http://prometheus:9090

**Loki**
- URL: http://loki:3100

**Tempo**
- URL: http://tempo:3200

## 설정 파일

- `conf/prometheus.yml`: Prometheus 스크래핑 설정
- `conf/loki.yml`: Loki 저장소 및 보존 정책
- `conf/tempo.yml`: Tempo 트레이싱 설정

## 데이터 보존 정책

- Prometheus: 7일
- Loki: 7일 (168시간)
- Tempo: 7일 (168시간)

## 주의사항

- 모니터링 스택은 dev/prod 네트워크에도 접근하여 메트릭을 수집합니다
- Spring Boot 애플리케이션에 `spring-boot-starter-actuator`와 `micrometer-registry-prometheus`가 필요합니다
