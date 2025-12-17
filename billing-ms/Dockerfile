# Etapa 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# El nombre del JAR debe coincidir con el artifactId y version de tu pom.xml
COPY --from=builder /app/target/billing-ms-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto 8081 (diferente al de orders)
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]