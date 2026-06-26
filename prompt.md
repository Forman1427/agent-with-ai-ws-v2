Этап 1: Первый основной промпт для составления каркаса приложения.

Комментарий: этот промпт был составлен не сразу, а после изучения возможных способов решения задячи - какие зависимости могут понадобиться для работы с WebSocket, базой данных, логироания, плагины; какая будет примерно структура приложения, какие возможно понадобятся классы, модели, dto и др.
После получения этой информации и часть ТЗ (связанная с "Агент с ИИ") был составлен данный промпт.

В процессе были использоваы только бесплатные версии AI: perplexity.ai и AI-агент OpenCode (также с Free моделями предоставленными по default OpenCode Zen: DeepSeek V4 Flash Free, MiMo V2.5 Free, Nemotron 3 Ultra Free, North Mini Code Free).


Сам промпт:


Ты эксперт многолетним опытом по: Kotlin, Ktor, Intellig idea, базам данных (в частности postgresql), gradle, docker, и другим связанным с ними технологиям.
Сгенерируй базовый каркас серверного приложения на Kotlin для тестового задания.

Требования к приложению:

1. Стек:
- Язык: Kotlin (JVM, Java 21).
- Фреймворк: Ktor (без Spring).
- Сборка: Gradle (Kotlin DSL, shadowJar для запуска fat-jar).
- Логирование: logback + kotlin-logging.
- Работа с БД: PostgreSQL + Exposed.
- Асинхронность: корутины, очереди, WebSocket.

2. Функциональность (агент с ИИ):
- Пользователь отправляет текстовое задание на сервер.
- Сервер создаёт задачу, сохраняет её в БД и возвращает ID.
- Асинхронный воркер обрабатывает задачу:
    - обращается к внешнему AI-сервису (можно через OpenRouter / mock-клиент),
    - имитирует прогресс выполнения (10–20–...–100 %),
    - по завершении сохраняет результат в БД.
- Пользователь может:
    - запросить список своих задач,
    - получить задачу по ID,
    - получать прогресс и результат через WebSocket.

3. Архитектура и слои:
- 'Main.kt' с 'Application.module()', где:
    - инициализируется подключение к БД,
    - создаются репозиторий задач, сервисы и очередь обработки задач,
    - настраиваются плагины Ktor (CORS, логирование, JSON, WebSocket, routing).
- Слой данных:
    - data class 'Task' с полями id (UUID), userName, taskText, status, progress, createdAt, completedAt?, resultText?,
    - enum 'TaskStatus' (QUEUED, IN_PROGRESS, COMPLETED, FAILED),
    - таблица 'TasksTable' на Exposed,
    - интерфейс 'TaskRepository' и реализация 'PostgresTaskRepository'.
- Сервисный слой:
    - 'TaskService' / 'TaskServiceImpl' для REST-операций (создание задачи, список задач пользователя, задача по id),
    - 'TaskProcessingService' / 'UserProcessingServiceImpl' для асинхронной обработки задач и обновления статусов,
    - очередь 'TaskProcessingQueue' / 'TaskProcessingQueueImpl' на корутинах и 'Channel<UUID>'.
- Интеграция с ИИ:
    - интерфейс 'AiServiceClient',
    - 'MockAiServiceClient', который ждёт искусственную задержку и возвращает тестовый текст,
    - 'RealAiServiceClient', который читает API-ключ из .env (через dotenv-kotlin) и вызывает внешний AI-API (например, OpenRouter) через Ktor HttpClient.

4. API:
- REST-эндпоинты (Ktor routing):
    - 'POST /api/v1/tasks' — создать задачу (body: userName, taskText), вернуть DTO с id, статусом и временем создания,
    - 'GET /api/v1/tasks?userName=...' — список задач пользователя, отсортированных по createdAt,
    - 'GET /api/v1/tasks/{taskId}' — получить задачу по id.
- WebSocket:
    - '/ws/tasks?userName=...' — клиент подключается и получает события:
        - TASK_STARTED,
        - TASK_PROGRESS (с процентом выполнения),
        - TASK_COMPLETED (с resultText и completedAt),
        - TASK_FAILED (с описанием ошибки).
    - Формат сообщений — JSON-DTO 'WsServerMessage'.

5. Технические детали:
- Конфиг 'application.conf':
    - блок 'database' (url, driver, user, password, maximumPoolSize),
    - блок 'processing' (количество воркеров, флаг useMockAi),
    - блок 'cors' (allowedHosts, режим development).
- Использовать kotlinx.serialization для JSON ('ContentNegotiation' + 'kotlinx.serialization.json').
- Локальное время для createdAt/completedAt — через 'kotlinx.datetime' ('Clock.System.now().toLocalDateTime(TimeZone.UTC)').
- Подготовить базовый 'index.html' в 'resources/static', который:
    - позволяет ввести userName и taskText,
    - отправить запрос на создание задачи,
    - открыть WebSocket и отображать прогресс/статус задач.

6. Вывод:
- Сгенерируй проект как набор файлов (build.gradle.kts, application.conf, исходники и т.д.) с готовой компилируемой структурой.
- Код должен быть разделён по пакетам: 'database', 'repository', 'model', 'dto', 'services', 'routes', 'plugins'.
- Следи за тем, чтобы зависимости в Gradle и импорты соответствовали друг другу и Ktor 3.x.

Если какие-то детали остаются на твой выбор, делай разумные решения по умолчанию, главное — чтобы проект можно было собрать и запустить.



--------------------------------------------------------
В результате была предолжена структура проекта:

```tsxt
ktor-ai-task-app/
├─ build.gradle.kts
├─ settings.gradle.kts
└─ src/main/
   ├─ kotlin/com/example/app/
   │  ├─ Main.kt
   │  ├─ database/
   │  │  └─ DatabaseFactory.kt
   │  ├─ model/
   │  │  ├─ Task.kt
   │  │  └─ TaskStatus.kt
   │  ├─ dto/
   │  │  ├─ TaskDtos.kt
   │  │  └─ WsMessages.kt
   │  ├─ repository/
   │  │  ├─ TaskRepository.kt
   │  │  └─ PostgresTaskRepository.kt
   │  ├─ services/
   │  │  ├─ AiServiceClient.kt
   │  │  ├─ MockAiServiceClient.kt
   │  │  ├─ RealAiServiceClient.kt
   │  │  ├─ AiServiceFactory.kt
   │  │  ├─ TaskService.kt
   │  │  ├─ TaskServiceImpl.kt
   │  │  ├─ TaskProcessingQueue.kt
   │  │  ├─ TaskProcessingQueueImpl.kt
   │  │  ├─ TaskWebSocketHub.kt
   │  │  ├─ TaskProcessingService.kt
   │  │  └─ TaskProcessingServiceImpl.kt
   │  ├─ routes/
   │  │  ├─ TaskRoutes.kt
   │  │  └─ WsRoutes.kt
   │  └─ plugins/
   │     ├─ Cors.kt
   │     ├─ Json.kt
   │     ├─ Logging.kt
   │     ├─ WebSockets.kt
   │     └─ ErrorHandling.kt
   └─ resources/
      ├─ application.conf
      └─ static/index.html
```

Были созданы файлы и пустые заглушки внутри: TODO: тут будет реализация...


---------------------------------------------------------------------------
Этап 2: редактирование приложения по рекомендации LLM - добавление в некоторых месстах логгирования, добавление JSON-сериализации (вместо строки)

На первом этапе взаимодействие с приложением было через Postman. Далее был сформирован index.html - для работы с приложением из браузера (тут также было достаточное количество вариантов промптов и результатов - вначале для одного запроса, потом форма поменялась - добавилась кнопка, таблица, различные варианты доработок JS-скриптов и т.д.).

В итоге был получен index.html файл для взаимодействия с приложением в браузере с достаточно user-friendly-интерфейсом.

---------------------------------------------------------------------------
Этап 3: реализация взаимодействия с реальной БД PostgreSQL

Отправил контракт TaskRepository.kt в LLM и попросил реализовать его с учётом моего стека технологий а также сгенерировать все необходимые дополнительные классы.

Далее генерация и добавление тестов;
Генерация docker-compose.yml файла для запуска субд postgres в контейнере.

---------------------------------------------------------------------------
Этап 4: Добавление реального взаимодействия с внешним AI-сервисом.

Генерация запроса: какие неронные сети, LLM предоставляют бесплатный API для уебных целей. В резудльтате нескольких рекомендаций и их проверок была выбрана связка: взаимедействие через openrouter.ai модель: DeepSeekR1Free

Генерация кода для взаимодействия с DeepSeekR1Free: напиши рабочий пример взаимодействия с DeepSeekR1Free со всеми необходимыми классами и настройками.

Далее для доведения до рабочего состояния (возникали различные ошибки) было взаимодействие с чатом - много мелких запросов-ответов.

Результат: рабочее взаимодействие приложения с реальным AI-сервисом 



