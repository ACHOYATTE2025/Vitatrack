# ── Stage 1: Build ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────────────
# Use eclipse-temurin:17-jre (Debian-based, NOT Alpine)
# Alpine uses musl libc which causes SIGSEGV with gRPC native libraries
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd -r vitatrack && useradd -r -g vitatrack vitatrack

COPY --from=build /app/target/*.jar app.jar

USER vitatrack

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/vitatrack/v1/actuator/health || exit 1

# Add JVM flags to help gRPC work correctly in containers
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dio.grpc.netty.shaded.io.netty.tcnative.assumeJni=false", \
  "-jar", "app.jar"]