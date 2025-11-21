FROM eclipse-temurin:11-jdk

WORKDIR /app

COPY . /app

CMD ["mvn", "verify", "-pl", "e2e-tests", "-am"]
