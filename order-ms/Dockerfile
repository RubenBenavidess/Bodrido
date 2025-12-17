# Etapa 1: Construcción (Build)
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiamos archivos de configuración de Maven
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Damos permisos de ejecución al wrapper y descargamos dependencias (para cachear esta capa)
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copiamos el código fuente y compilamos
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Etapa 2: Imagen Final (Runtime)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos solo el JAR compilado desde la etapa anterior
# El nombre del jar depende de tu pom.xml (artifactId-version)
COPY --from=builder /app/target/order-ms-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]