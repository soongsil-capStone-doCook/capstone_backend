# [실행용 Dockerfile]
FROM amazoncorretto:17

WORKDIR /app

# 아까 scp로 보낸 app.jar를 컨테이너 안으로 복사
COPY app.jar app.jar

# 프로파일 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 바로 실행
ENTRYPOINT ["java", "-jar", "app.jar"]