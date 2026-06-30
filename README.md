# Task Processor

Сервер асинхронной обработки задач с AI. Пользователь отправляет задание через REST API, сервер обрабатывает его (через AI-клиент) и отправляет прогресс/результат через WebSocket.

## Стек

- **Kotlin** + **Ktor** (HTTP + WebSocket)
- **PostgreSQL** + **Exposed ORM** (или In-Memory)
- **Gradle** + Shadow JAR
- **Docker**

## Краткая инструкция по сборке и запуску

### Требования

- Установленная Java 21+ с настроенными переменными окружения
- Docker
- образ в docker PostgreSQL 17 (в приложении используется версия 17.6. Если такого образа нет, то при запуске команды `docker-compose up postgres` скачается автоматически или можно поменять на свою версию 17.х в файле docker-compose)
- (_опционально_) установленная IDE (например IntelliJ IDEA) для удобного перемещения по проекту, запуску приложения, изменения конфигурации, просмотра файлов и т.д.

!!! Далее будут представлены команды которые можно запускать вне IDE (например в Git Bash)

### 1. Сборка fat-jar локально

fat-jar - это jar-файл уже со всеми необходимыми зависимостями

```bash
./gradlew clean shadowJar
```
Команду ./gradlew clean shadowJar нужно выполнять в корне проекта, там, где лежит build.gradle.kts (и файл gradlew)

- Получим `build/libs/taskprocessor.jar` 


### 2. Конфигурация и запуск приложения

#### Варианты конфигурации запуска приложения

Файл `application.conf` в блоке processing содержит два параметра: useMockAi и useInMemoryRepository которые можно задать перед запуском приложения (описание каждого см. далее):

```hocon
processing {
  workerCount = 2
  useMockAi = true
  useInMemoryRepository = true
}
```

#### Выбор AI-клиента


#### Сценарии локального запуска (без Docker)

| Режим                         | useMockAi | useInMemoryRepository | Требуется AI_API_KEY | Команда                                   |
|------------------------------|-----------|------------------------|----------------------|-------------------------------------------|
| Mock AI + In-Memory          | true      | true                   | нет                  | `java -jar build/libs/taskprocessor.jar` |
| Real AI + In-Memory          | false     | true                   | да                   | `AI_API_KEY=... java -Dprocessing.useMockAi=false -jar ...` |
| Mock AI + Postgres           | true      | false                  | нет                  | `java -Dprocessing.useInMemoryRepository=false -jar ...`   |
| Real AI + Postgres           | false     | false                  | да                   | `AI_API_KEY=... java -Dprocessing.useMockAi=false -Dprocessing.useInMemoryRepository=false -jar ...` |


Кратко по каждому параметру:
#### Выбор реализации AI-сервиса

| `useMockAi`       | Что используется                                                                     |
|-------------------|--------------------------------------------------------------------------------------|
| `true` (default*) | MockAiServiceClient (имитация — 20 сек задержка, готовый ответ)                      |
| `false`           | RealAiServiceClient (запрос к DeepSeek через OpenRouter) + нужно передать AI_API_KEY |

#### Настройка AI_API_KEY
Ключ для доступа к внешнему AI-сервису (OpenRouter / DeepSeek) задаётся только через переменную окружения `AI_API_KEY`.

- локально: `AI_API_KEY=... java -Dprocessing.useMockAi=false -jar ...`
- в Docker: значение задаётся в `.env` и подтягивается в контейнер через `docker-compose.yml`.

Если `useMockAi = true`, ключ не используется.


#### Выбор реализации (способа) хранения информации

| `useInMemoryRepository` | Что используется                                                     |
|-------------------------|----------------------------------------------------------------------|
| `true` (default)        | InMemoryTaskRepository (Использование IN-Memory вариант репозитория) |
| `false`                 | PostgresTaskRepository (использование СУБД PostgreSQL)**             |

\* default - это значение уже задано в файле `application.conf`

\** В случаях использования СУБД PostgreSQL - она запускается в docker-контейнере и её нужно запустить заранее - до запуска приложения (инструкция запуска смотри ниже).


##### Запуск PostgreSQL в docker-контейнере 
Чтобы запустить PostgreSQL в контейнере нужно выполнить команду: `docker-compose up postgres`

Для запуска команды `docker-compose up postgres` нужно находиться в той папке, где лежит `docker-compose.yml`, и запускать команду оттуда

Или, находясь в другой папке, можно указать файл `docker-compose.yml` явно:

`docker-compose -f C:\path\to\project-folder\docker-compose.yml up postgres`


##### С учётом конфигурации есть несколько вариантов запустить приложение локально (не в docker):

1.  Mock AI + In-Memory:

```text 
Будут использованы следующие параметры:
useMockAi = true
useInMemoryRepository = true
```

! У нас уже должен быть собран JAR-файл (шаг 1)

```text 
java -jar build/libs/taskprocessor.jar
```
Далее можно открыть файл `index.html` (по следующему адресу:  `http://localhost:8080/static`)



2. Real AI + In-Memory: (+ нужно передать AI_API_KEY)

```text 
Будут использованы следующие параметры:
useMockAi = false
useInMemoryRepository = true
+ нужно передать AI_API_KEY
```

У нас в корне приложения* есть `.env` файл:
```text
AI_API_KEY=ВАШ_КЛЮЧ_ЗДЕСЬ
```

\* Если у вас только JAR-файл, то поместите `.env` файл рядом с JAR-файлом в одном каталоге.

```text
java -Dprocessing.useMockAi=false  -jar build\libs\taskprocessor.jar
```



3.  Mock AI + Postgres (!!! До запуска приложения в docker должен быть запущен контейнер с СУБД Postgres. См. инструкцию запуска выше `Запуск PostgreSQL в docker-контейнере`)

```text 
Будут использованы следующие параметры:
useMockAi = true
useInMemoryRepository = false
```

```text
java -Dprocessing.useInMemoryRepository=false  -jar build\libs\taskprocessor.jar
```



4.  Real AI + Postgres (нужно и передать AI_API_KEY и в docker должна быть поднята СУБД Postgres до запуска приложения)

```text 
Будут использованы следующие параметры:
useMockAi = false
useInMemoryRepository = false
+ нужно передать AI_API_KEY
```

У нас в корне приложения* есть `.env` файл:
```text
AI_API_KEY=ВАШ_КЛЮЧ_ЗДЕСЬ
```

\* Если у вас только JAR-файл, то поместите `.env` файл рядом с JAR-файлом в одном каталоге.

```text
java -Dprocessing.useMockAi=false  -jar build\libs\taskprocessor.jar
```

```text
java -Dprocessing.useMockAi=false -Dprocessing.useInMemoryRepository=false  -jar build\libs\taskprocessor.jar
```




##### С учётом конфигурации запуск всего через docker-compose (app + postgres):

Для запуска в Docker продемонстрируем возможность задать конфигурацию в файле `.env`
Строки можно удалять полностью или закомментировать знаком # или наоборот расскомментировать (убрать #)

ВНИМАНИЕ! **Значения из .env могут быть перекрыты переменными окружения в оболочке**

_Например_: Если вы зададите переменную окружения PROCESSING_USE_MOCK_AI в своей оболочке, она перекроет значение из .env. Для предсказуемого поведения лучше либо править .env, либо явно задавать значения в командной строке.

1. Real AI + Postgres:

`.env: `
```text
AI_API_KEY="secret"
PROCESSING_USE_MOCK_AI=false
PROCESSING_USE_IN_MEMORY_REPOSITORY=false 
```
secret должен быть заменён на реальный ключ

```bash
# запуск приложения
docker compose up --build
```

- Приложение — в контейнере app, подключается к Postgres по jdbc:postgresql://postgres:5432/...



2.  Mock AI + Postgres:

`.env: `
```text
AI_API_KEY=""  # можно оставить пустым или вообще удалить
PROCESSING_USE_MOCK_AI=true
PROCESSING_USE_IN_MEMORY_REPOSITORY=false
```

```bash
# запуск приложения
docker compose up --build
```


3. Mock AI + In-Memory (чистый dev):

`.env: `
```text
AI_API_KEY=""  # можно оставить пустым или вообще удалить
PROCESSING_USE_MOCK_AI=true
PROCESSING_USE_IN_MEMORY_REPOSITORY=true
```
```bash
# запуск приложения
docker compose up --build
```

В этом режиме:
контейнер postgres поднимается, но репозиторий в приложении выбирается InMemoryTaskRepository (по useInMemoryRepository=true);


4. Real AI + In-Memory:

`.env: `
```text
AI_API_KEY="secret"
PROCESSING_USE_MOCK_AI=false
PROCESSING_USE_IN_MEMORY_REPOSITORY=true 
```
secret должен быть заменён на реальный ключ

```bash
# запуск приложения
docker compose up --build
```


5. Переопределение «на лету», без правки .env

Иногда удобно разово запустить с другим набором параметров, не трогая .env.

Вариант: env‑переменные при запуске

```bash
PROCESSING_USE_MOCK_AI=true \
PROCESSING_USE_IN_MEMORY_REPOSITORY=true \
AI_API_KEY= \
docker compose up --build
```

-----------------------

### Приоритет конфигураций (при наличии нескольких) (сверху вниз)

| Уровень | Пример | Откуда |
|---|---|---|
| 1. `-D` флаг | `-DPROCESSING_USE_MOCK_AI=false` | Системное свойство |
| 2. OS env | `$env:PROCESSING_USE_MOCK_AI="false"` | Переменная окружения |
| 3. `.env` файл | `PROCESSING_USE_MOCK_AI=false` | Через dotenv |
| 4. `application.conf` | `useMockAi = true` | Файл конфига |

-----------------------
- Compose соберёт образ `taskprocessor-app`, скопирует `taskprocessor.jar` внутрь и запустит его.
- `app` дождётся, пока `postgres` станет healthy (по `pg_isready`), затем стартует.


![Пример запуске всего в Docker](img/1%20docker-full.png)

Сервер стартует на `http://localhost:8080`

Запустить тестовую стартовую html-страничку можно по следующему адресу:  `http://localhost:8080/static`.


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
- Отправить - отправить задание на исполнение
- Мои задачи - показать отправленные задачи (доступна если есть отправленные задачи)

Чтобы отправить задание на выполнение нужно заполнить имя и Описать задание и далее нажать на "Отправить"
![alt-текст](img/3%20new-task.png)

После выведется форма с процессом выполнения задания:
![alt-текст](img/4%20task-in-process.png)
На этой форме доступны два действия:
- Мои задачи - показать отправленные задачи (доступка если есть отправленные задачи)
- Создать новое - перейти на форму Создания новой задачи

Перейдём в Мои задачи
![alt-текст](img/5%20my-tasks-1.png)
Видим информацию по задача и прогресс её выполения (процент выполнения меняется динамически). Как только задача будет выполнена данные обновятся:
![alt-текст](img/6%20my-tasks-is-done.png)


Приложение искусственно ограничено одновременным выполнением двух задач - как результат мы видим очередь задач:
![alt-текст](img/7%20queue-tasks.png)
Как только задачи, которые находятся в состоянии "В процессе" выполнятся выполнение переходит к следующим задачам в очереди:
![alt-текст](img/8%20queue-tasks-2.png)
И так до тех пор пока все задачи не будут выполнены
![alt-текст](img/9%20queue-tasks-is-done.png)

Также можем посмотреть результат выполнения задачи выбрав её из списка "Мои задачи" и нажав на против неё кнопку "открыть". Выведется полная информация о задаче с результатом выполнения:
![alt-текст](img/10%20result-of-task.png)

