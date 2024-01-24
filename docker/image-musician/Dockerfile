FROM openjdk:21

WORKDIR /app
COPY ./target/musician.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]