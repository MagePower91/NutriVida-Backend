
# Imagen de ms-clinico: compilación y ejecución con Java 21.

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./

# Descargar dependencias para aprovechar la caché de Docker.
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=compilacion /app/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
