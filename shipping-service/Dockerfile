
FROM openjdk:11
ARG PROJECT_VERSION=0.1.0
ENV SPRING_PROFILES_ACTIVE=dev
WORKDIR /home/app
COPY target/shipping-service-v${PROJECT_VERSION}.jar shipping-service.jar
EXPOSE 8704
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "shipping-service.jar"]


