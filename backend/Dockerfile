FROM maven:3.9.14-eclipse-temurin-21 AS dev
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY scripts ./scripts
COPY src ./src
EXPOSE 8080
CMD ["sh", "./scripts/dev.sh"]

FROM dev AS build
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S articleshelf && adduser -S -G articleshelf articleshelf
COPY --from=build --chown=articleshelf:articleshelf /workspace/target/articleshelf-backend-0.1.0.jar app.jar
USER articleshelf
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
