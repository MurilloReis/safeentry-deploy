# Usamos uma imagem base do OpenJDK 17 (Java 17) para rodar sua aplicação Spring Boot
FROM openjdk:17-jdk-slim

# Define o volume para guardar os logs (útil para logs de container)
VOLUME /tmp

# Argumento para especificar o arquivo JAR que será construído pelo Maven/Gradle
# Assume que o JAR estará dentro da pasta 'target/' após a compilação do seu projeto Spring Boot
ARG JAR_FILE=target/*.jar

# Copia o arquivo JAR da sua aplicação para dentro da imagem Docker
# O '*' no target/*.jar significa que ele pegará qualquer arquivo .jar dentro da pasta target
COPY ${JAR_FILE} app.jar

# Define o comando que será executado quando o contêiner for iniciado
# Isso inicia sua aplicação Spring Boot
ENTRYPOINT ["java","-jar","/app.jar"]

# Exponha a porta que sua aplicação Spring Boot escuta (geralmente 8080)
# Isso informa ao Docker que esta porta será usada pelo contêiner
EXPOSE 8080