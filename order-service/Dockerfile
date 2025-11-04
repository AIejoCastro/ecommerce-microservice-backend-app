
FROM eclipse-temurin:11-jre
ARG PROJECT_VERSION=0.1.0
ENV SPRING_PROFILES_ACTIVE=dev
WORKDIR /home/app
COPY target/order-service-v${PROJECT_VERSION}.jar order-service.jar
EXPOSE 8702
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "order-service.jar"]


