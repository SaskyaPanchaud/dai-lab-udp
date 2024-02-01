FROM openjdk:21

WORKDIR /app
COPY ./target/auditor.jar /app/app.jar

EXPOSE 2205

ENTRYPOINT ["java", "-jar", "/app/app.jar"]