FROM eclipse-temurin:11-jdk
RUN mkdir -p /home/app
WORKDIR /home/app
COPY target/user-service-v0.1.0.jar user-service.jar
EXPOSE 8700
ENTRYPOINT ["java", "-jar", "user-service.jar"]
