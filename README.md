# Task Processor

Сервер асинхронной обработки задач с AI. Пользователь отправляет задание через REST API, сервер обрабатывает его (через AI-сервис) и отправляет прогресс/результат через WebSocket.

## Стек

- **Kotlin** + **Ktor** (HTTP + WebSocket)
- **PostgreSQL** + **Exposed ORM** (или In-Memory)
- **Gradle** + Shadow JAR
- **Docker**

## Краткая инструкция по сборке и запуску

### Требования

- Установленная JDK 25 с настроенными переменными окружения
- PostgreSQL 17 (или `docker-compose up postgres`)

### 1. Сборка fat-jar локально

```bash
./gradlew clean shadowJar
```
Команду ./gradlew clean shadowJar нужно выполнять в корне проекта, там, где лежит build.gradle.kts (и файл gradlew)
 

- Получим `build/libs/taskprocessor.jar` 

### 2. Запуск приложения локально с БД в Docker

```bash
# сначала поднимаем только postgres
docker compose up -d postgres

# запуск приложения
java -jar build/libs/taskprocessor.jar
```

- БД слушает на `localhost:5433` (по `POSTGRES_HOST_PORT` в compose), а приложение использует `DB_*` переменные.


### 3. Запуск всего через docker-compose (app + postgres)

```bash
# 1) собираем fat-jar
./gradlew clean shadowJar

# 2) поднимаем всё в docker (образ app собирается на основе Dockerfile)
docker compose up --build
```

- Compose соберёт образ `taskprocessor-app`, скопирует `taskprocessor.jar` внутрь и запустит его.
- `app` дождётся, пока `postgres` станет healthy (по `pg_isready`), затем стартует.

![Скриншот приложения](img/ screenshot-1.png)

![alt-текст](img/1%20docker-full.png)

Сервер стартует на `http://localhost:8080`


Запустить тестовую стартовую html-страничку можно по следующему адресу:  `http://localhost:8080/static`.




### Варианты конфигурации запуска приложения

## Конфигурация

Файл `application.conf` и `application-docker.conf` в зависимости от способа запуска приложения - локально или в docker-контейнере:

```hocon
processing {
  workerCount = 2
  useMockAi = true
  useInMemoryRepository = true
}
```

#### Выбор AI-клиента

| `useMockAi`      | Что используется |
|------------------|---|
| `true` (default) | MockAiServiceClient (имитация — 20 сек задержка, готовый ответ) |
| `false`          | RealAiServiceClient (запрос к DeepSeek через OpenRouter) |


#### Выбор AI-клиента

| `useInMemoryRepository` | Что используется                                                     |
|-------------------------|----------------------------------------------------------------------|
| `true` (default)        | InMemoryTaskRepository (Использование IN-Memory вариант репозитория) |
| `false`                 | PostgresTaskRepository (использование СУБД PostgreSQL)               |



## Способы выполнения запросов
##  REST API

### Создать задачу

```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"userName": "maksim", "taskText": "Напиши краткое резюме текста"}'
```

Ответ:
```json
{
  "id": "uuid",
  "userName": "maksim",
  "taskText": "Напиши краткое резюме текста",
  "status": "QUEUED",
  "progress": 0,
  "createdAt": "2026-06-29T12:00:00"
}
```

### Получить задачи пользователя

```bash
curl http://localhost:8080/api/v1/tasks?userName=maksim
```

### Получить задачу по ID

```bash
curl http://localhost:8080/api/v1/tasks/{id}
```

## WebSocket

Подключитесь к WebSocket, указав имя пользователя:

```bash
websocat ws://localhost:8080/ws/tasks?userName=maksim
```

После создания задачи сервер будет отправлять сообщения:

```json
{ "type": "TASK_STARTED",  "taskId": "uuid", "status": "IN_PROGRESS", "progress": 0,  "message": "Начали обработку задачи" }
{ "type": "TASK_PROGRESS", "taskId": "uuid", "status": "IN_PROGRESS", "progress": 50, "message": "Прогресс: 50%" }
{ "type": "TASK_COMPLETED","taskId": "uuid", "status": "COMPLETED",  "progress": 100,"message": "Задача выполнена", "resultText": "..." }
```


## Отправка запросов из index.html 

После запуска приложения, запускаем страничку index.html (по следующему адресу:  `http://localhost:8080/static`) и нам сразу высветиться форма:

![alt-текст](img/2%20main-form.png)

Доступны два действия:
- Отправить - отправить задание на испольнение
- Мои задачи - показать отправленные задачи (доступка если есть отправленные задачи)

Чтобы отправить задание на выполнение нужно заполнить имя и Описать задние и далее нажать на "Отправить"
![alt-текст](img/3%20new-task.png)

После выведется форма с процессом выполнения задания:
![alt-текст](img/4%20task-in-process.png)
На этой форме доступны два действия:
- Мои задачи - показать отправленные задачи (доступка если есть отправленные задачи)
- Создать новое - перейти на форму Создания новой задачи

Перейдём в Мой задачи
![alt-текст](img/5%20my-tasks-1.png)
Видим информацию по задача и прогресс её выполения (процент выполнения меняется динамически). Как только задача будет выполнена данные обновятся:
![alt-текст](img/6%20my-tasks-is-done.png)


Приложение исскуственно ограничено одновременным выполнением двух задач - как результат мы видим очередь задач:
![alt-текст](img/7%20queue-tasks.png)
Как только задачи, которые находятся в состоянии "В процессе" выполнятся выполнение переходит к следующим задачам в очереди:
![alt-текст](img/8%20queue-tasks-2.png)
И так до тех пор пока все задачи не будут выполнены
![alt-текст](img/9%20queue-tasks-is-done.png)

Также можем посмотреть результат выполнения задачи выбрав её из списка "Мои задачи" и нажав на против неё кнопку "открыть". Выведется полная информация о задаче с результатом выпонения:
![alt-текст](img/10%20result-of-task.png)

