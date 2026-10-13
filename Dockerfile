FROM eclipse-temurin:21-jre-jammy
ARG JAR_FILE=build/libs/lms-backend-0.0.1-SNAPSHOT.jar
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --chown=app:app ${JAR_FILE} /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
