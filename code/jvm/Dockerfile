FROM gradle:8.10-jdk21 AS build
WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew :host:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/host/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]