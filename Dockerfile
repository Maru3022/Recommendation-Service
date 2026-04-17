# Этап сборки
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

# Этап запуска
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Копируем jar с суффиксом -exec, так как он указан в pom.xml
COPY --from=build /app/target/*-exec.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]