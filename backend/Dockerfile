FROM maven:3.9.14-eclipse-temurin-21 AS dev
WORKDIR /workspace
COPY pom.xml .
RUN for attempt in 1 2 3; do \
      mvn -B -ntp -DskipTests dependency:go-offline && break; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "Maven go-offline failed; retrying ($attempt/3)"; \
      sleep 5; \
    done
COPY scripts ./scripts
COPY src ./src
EXPOSE 8080
CMD ["sh", "./scripts/dev.sh"]

FROM dev AS build
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S articleshelf && adduser -S -G articleshelf articleshelf
COPY --from=build --chown=articleshelf:articleshelf /workspace/target/articleshelf-backend-0.1.0.jar app.jar
USER articleshelf
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
