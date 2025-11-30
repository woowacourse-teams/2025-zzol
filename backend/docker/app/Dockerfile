# ================================================
# Coffee Shout Backend - Spring Boot Application
# ================================================
# Build: docker build -t coffee-shout-backend:latest .
# Run:   docker run -p 8080:8080 coffee-shout-backend:latest

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Coffee Shout Team"
LABEL description="Coffee Shout Backend Spring Boot Application"

# 작업 디렉토리 설정
WORKDIR /app

# 보안: non-root 유저 생성 및 설정
RUN addgroup -S spring && \
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
HEALTHCHECK --interval=5s \
    --timeout=3s \
    --start-period=40s \
    --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM 옵션 및 애플리케이션 실행
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/logs/heapdump.hprof", \
    "-Duser.timezone=Asia/Seoul", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
