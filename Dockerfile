FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*-exec.jar app.jar
RUN chown spring:spring /app/app.jar
USER spring
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
