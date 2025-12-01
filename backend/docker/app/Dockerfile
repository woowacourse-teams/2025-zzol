# ================================================
# ZZOL Backend - Spring Boot Application
# ================================================
# Build: docker build -t coffee-shout-backend:latest .
# Run:   docker run -p 8080:8080 coffee-shout-backend:latest

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ZZOL Team"
LABEL description="ZZOL Backend Spring Boot Application"

# 작업 디렉토리 설정
WORKDIR /app

# curl 설치 (헬스체크용) + non-root 유저 생성
RUN apk add --no-cache curl && \
    addgroup -S spring && \
    adduser -S spring -G spring && \
    mkdir -p /app/logs && \
    chown -R spring:spring /app

# JAR 파일 복사 (GitHub Actions에서 빌드된 JAR)
# docker build 시 context에서 복사됨
COPY --chown=spring:spring *.jar app.jar

# spring 유저로 전환
USER spring:spring

# 애플리케이션 포트
EXPOSE 8080

# Health check 설정
# curl은 Alpine Linux에 기본 포함됨
HEALTHCHECK --interval=30s \
    --timeout=3s \
    --start-period=40s \
    --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 옵션 및 애플리케이션 실행
# 환경변수로 힙 메모리 설정 가능 (기본값: dev 환경)
ENV JAVA_OPTS="-Xms1g -Xmx1536m"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Duser.timezone=Asia/Seoul \
    -Djava.security.egd=file:/dev/./urandom \
    -jar app.jar"]
