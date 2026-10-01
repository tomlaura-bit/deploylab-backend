FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r deploylab && useradd -r -g deploylab deploylab
COPY --from=build /build/target/deploylab-backend-1.0.0.jar app.jar
USER deploylab
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
