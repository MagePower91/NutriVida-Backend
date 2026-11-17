# Imagen de ms-clinico. Se construye en dos etapas:
# 1) compila el proyecto con Maven (JDK 21) y 2) ejecuta solo el .jar con un JRE 21, más liviano.

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Las dependencias se descargan en una capa aparte para no repetirlo si solo cambia el código.
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
