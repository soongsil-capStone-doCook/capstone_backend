# 1. Build Stage (Gradle 빌드)
FROM gradle:jdk17-alpine AS builder
WORKDIR /app

# 의존성 캐싱을 위해 설정 파일만 먼저 복사
COPY build.gradle settings.gradle ./
# 소스 코드 복사
COPY src ./src

# 빌드 실행 (테스트 제외하여 속도 향상)
# Gradle Wrapper가 있다면 ./gradlew, 없다면 gradle 명령 사용
RUN gradle clean bootJar -x test

# 2. Run Stage (실제 실행 환경)
FROM openjdk:17-jdk-slim
WORKDIR /app

# 빌드된 JAR 파일만 복사 (이미지 크기 최적화)
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행 시 'prod' 프로파일 적용
ENV SPRING_PROFILES_ACTIVE=prod

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]