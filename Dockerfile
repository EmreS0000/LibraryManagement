# Stage 1: Build (Jenkins sırasında)
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Build'de hafıza limit (Jenkins'deki MAVEN_OPTS ile uyumlu)
ENV MAVEN_OPTS="-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

COPY pom.xml .
RUN mvn dependency:resolve -q

COPY src ./src
RUN mvn clean package -DskipTests -q && rm -rf /root/.m2/repository

# Stage 2: Runtime (Docker container çalışırken)
FROM eclipse-temurin:21-jdk-alpine

# Runtime'da hafıza limit (docker-compose'daki mem_limit ile uyumlu)
ENV JAVA_OPTS="-Xmx320m -Xms160m -XX:MaxMetaspaceSize=64m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

WORKDIR /app
VOLUME /tmp

COPY --from=build /app/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]