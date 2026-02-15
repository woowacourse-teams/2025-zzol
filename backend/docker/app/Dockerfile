# ================================================
# ZZOL Backend - Spring Boot Application
# ================================================
# Build: docker build -t coffee-shout-backend:latest .
# Run:   docker run -p 8080:8080 coffee-shout-backend:latest

# Stage 1: JAR 레이어 추출
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /extract
COPY *.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# Stage 2: 최종 이미지
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ZZOL Team"
LABEL description="ZZOL Backend Spring Boot Application"

# 작업 디렉토리 설정
WORKDIR /app

# non-root 유저 생성
RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    mkdir -p /app/logs && \
    chown -R spring:spring /app

# 레이어별 복사 (변경 빈도 낮은 순서 → 캐시 효율 극대화)
COPY --from=extract --chown=spring:spring /extract/extracted/dependencies/ ./
COPY --from=extract --chown=spring:spring /extract/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=spring:spring /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=spring:spring /extract/extracted/application/ ./

# spring 유저로 전환
USER spring:spring

# 애플리케이션 포트
EXPOSE 8080

# Health check 설정 (wget은 Alpine에 기본 내장)
HEALTHCHECK --interval=30s \
    --timeout=3s \
    --start-period=40s \
    --retries=3 \
    CMD wget --quiet --spider http://localhost:8080/actuator/health || exit 1

# JVM 옵션 및 애플리케이션 실행
# 환경변수로 힙 메모리 설정 가능 (기본값: dev 환경)
ENV JAVA_OPTS="-Xms1400m -Xmx1400m"

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} \
    -XX:+UseG1GC \
    -XX:MaxMetaspaceSize=256m \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Duser.timezone=Asia/Seoul \
    org.springframework.boot.loader.launch.JarLauncher"]
