# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS frontend
WORKDIR /workspace/web
COPY web/package*.json ./
RUN npm ci --no-audit --no-fund
COPY web ./
COPY src/main/resources/static/styles.css ../src/main/resources/static/styles.css
RUN npm run test && npm run build

FROM --platform=$BUILDPLATFORM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
COPY --from=frontend /workspace/target/web ./target/web
RUN mvn -B -Dfrontend.skip=true verify

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S detour && adduser -S detour -G detour
WORKDIR /app
COPY --from=build /workspace/target/detour-*.jar app.jar
USER detour
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=25s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
