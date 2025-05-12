FROM openjdk:17

WORKDIR /app

COPY target/CloudStorage-0.0.1-SNAPSHOT.jar /app/cloudstorage.jar

ENTRYPOINT ["java", "-jar", "/app/cloudstorage.jar"]
