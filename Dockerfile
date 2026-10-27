# 온약 Spring 서버
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 레이어를 먼저 받아 두면 소스만 바뀌었을 때 다시 받지 않는다
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
