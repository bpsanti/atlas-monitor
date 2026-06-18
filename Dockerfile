FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json ./
RUN npm install
COPY frontend/src/ src/
COPY frontend/public/ public/
COPY frontend/angular.json frontend/tsconfig.json frontend/tsconfig.app.json ./
RUN npx ng build --configuration=production

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
COPY --from=frontend /app/frontend/dist/frontend/browser/ src/main/resources/static/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
