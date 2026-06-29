# Dockerfile

FROM eclipse-temurin:21-jre
LABEL maintainer="you <you@example.com>"

WORKDIR /app

# копируем fat-jar, ожидаем что он уже собран локально командой `./gradlew shadowJar`
COPY build/libs/taskprocessor.jar /app/taskprocessor.jar

# опциональный конфиг для docker-окружения (можно использовать внутри приложения при желании)
COPY src/main/resources/application-docker.conf /app/application-docker.conf

# дефолтные значения для подключения к БД внутри docker-сети
ENV DB_URL="jdbc:postgresql://postgres:5432/taskprocessor_db"
ENV DB_USER="taskprocessor"
ENV DB_PASSWORD="taskprocessor"

# базовые JVM-опции, можно переопределить при запуске контейнера
ENV JAVA_OPTS="-Xms256m -Xmx512m -Duser.timezone=UTC"

EXPOSE 8080

# healthcheck: проверяем что HTTP корневой эндпоинт доступен
# если в образе нет curl — healthcheck можно убрать или заменить
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD curl -f http://localhost:8080/ || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/taskprocessor.jar"]