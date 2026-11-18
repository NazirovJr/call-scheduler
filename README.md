# Call Scheduler

REST API для учёта звонков и назначения встреч — основа внутренней CRM отдела обзвона.

Java 21 · Spring Boot 4.1 · Spring Data JPA · PostgreSQL 17 · Flyway · Testcontainers

## Запуск

Нужны Git и Docker с Compose v2. Проверить, что они есть:

```bash
git --version
docker compose version
```

Если команда не найдена — установка на чистую систему:

<details>
<summary>Windows 10/11</summary>

В PowerShell от имени администратора:

```powershell
wsl --install --no-distribution
```

Перезагрузите компьютер и снова в PowerShell:

```powershell
winget install -e --id Git.Git
winget install -e --id Docker.DockerDesktop
```

Запустите Docker Desktop из меню «Пуск», дождитесь статуса «Engine running» и откройте новое окно
PowerShell. Если `winget` не найден — обновите «Установщик приложений» (App Installer) в Microsoft Store.

</details>

<details>
<summary>macOS</summary>

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Установщик Homebrew заодно поставит Git (в составе Xcode Command Line Tools). Если в конце он выведет
команды в блоке «Next steps» — выполните их, чтобы заработал `brew`. Затем:

```bash
brew install --cask docker-desktop
open -a Docker
```

Дождитесь, пока Docker Desktop покажет статус «Engine running».

</details>

<details>
<summary>Ubuntu / Debian</summary>

```bash
sudo apt update && sudo apt install -y git curl
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
```

Выйдите из системы и войдите снова, чтобы Docker работал без `sudo`.

</details>

Затем:

```bash
git clone https://github.com/NazirovJr/call-scheduler.git
cd call-scheduler
docker compose up --build
```

Первая сборка занимает несколько минут. API поднимется на `http://localhost:8080`, схема БД и три
менеджера (id 1–3) создаются миграциями из [src/main/resources/db/migration](src/main/resources/db/migration).
Проверка без дополнительных программ: откройте `http://localhost:8080/companies` в браузере — вернётся пустой список.

Примеры запросов — в [requests.http](requests.http) (открывается в IntelliJ IDEA и VS Code REST Client).
Сброс данных: `docker compose down`.

## Тесты

Нужны JDK 21 и запущенный Docker (его установка — выше): тесты работают с настоящим PostgreSQL через
Testcontainers. Maven ставить не нужно — его скачает `mvnw`. Проверить JDK:

```bash
java -version
```

Если команда не найдена или версия ниже 21:

| ОС | Установка JDK 21 |
|---|---|
| Windows | `winget install -e --id EclipseAdoptium.Temurin.21.JDK` |
| macOS | `brew install --cask temurin@21` |
| Ubuntu 20.04+ / Debian 13+ | `sudo apt install -y openjdk-21-jdk` |

Затем в новом терминале из папки проекта:

```bash
./mvnw test
```

В Windows PowerShell — `.\mvnw.cmd test`.

## API

Время принимается и отдаётся в UTC (ISO 8601), рабочие часы проверяются по `Asia/Almaty`.

| Метод | Путь | Тело / параметры | Успех |
|---|---|---|---|
| POST | `/companies` | `{"name", "bin", "phone"}` | 201 |
| GET | `/companies?status=&page=&size=` | `status`: `new`, `no_answer`, `refused`, `meeting_set` | 200 |
| POST | `/calls` | `{"companyId", "result", "meetingId"}`, `result`: `no_answer`, `refused`, `meeting_set` | 201 |
| POST | `/meetings` | `{"companyId", "managerId", "startsAt"}` | 201 |
| PATCH | `/meetings/{id}` | `{"action": "reschedule", "startsAt"}` или `{"action": "cancel"}` | 200 |
| GET | `/managers/{id}/schedule?date=` | `date`: `YYYY-MM-DD`, день по Алматы | 200 |

Отказы возвращаются в формате `application/problem+json` с понятным текстом в `detail`:

```json
{"status": 409, "title": "Conflict", "instance": "/meetings",
 "detail": "У менеджера 1 уже есть встреча, пересекающаяся с этим временем"}
```

| Код | Когда |
|---|---|
| 400 | Некорректные поля, встреча вне рабочих часов или в прошлом, `meeting_set` без встречи |
| 404 | Организация, менеджер или встреча не найдены |
| 409 | Пересечение у менеджера, вторая активная встреча организации, повторный БИН, встреча уже отменена или прошла |

## Бизнес-правила

| № | Правило | Где реализовано |
|---|---|---|
| 1 | Встреча длится 60 минут | `Meeting.DURATION`, `endsAt` вычисляется сервером |
| 2 | Только пн–пт 09:00–18:00 по Алматы, целиком | `WorkingHours` |
| 3 | У менеджера нет пересекающихся встреч, встык — можно | `MeetingService.requireManagerFree` + exclusion constraint в БД |
| 4 | У организации одна активная встреча | `MeetingService.create` под блокировкой строки организации |
| 5 | `meeting_set` только с созданной встречей | `CallService.register` + CHECK в БД |
| 6 | Перенос не теряет историю | таблица `meeting_reschedules`, неизменяемый `createdAt`, поле `history` в ответе |
| 7 | Отказ — корректный код и сообщение | `ApiExceptionHandler` |

Одновременные запросы не нарушают правила 3 и 4: проверки выполняются под блокировкой строк
организации и менеджера, а пересечения у менеджера дополнительно запрещает сама БД.

## Допущения

ТЗ оставляет несколько вопросов открытыми — они решены так:

- **Статус организации** — результат последнего звонка (`new`, пока не звонили). При отмене встречи,
  о которой был звонок `meeting_set`, организация возвращается из `meeting_set` в `new`.
- **Активная встреча** — не отменена и ещё не закончилась. Прошедшая встреча не мешает назначить новую.
- **Встречу нельзя назначить или перенести на прошедшее время** — иначе правило 4 обходилось бы задним числом.
- **Переносить и отменять можно только активную встречу**; менеджер при переносе не меняется.
- **Звонок `meeting_set`** ссылается на активную встречу той же организации через `meetingId`:
  сначала `POST /meetings`, затем `POST /calls`. Для других результатов `meetingId` не допускается.
- **Расписание** отдаёт все встречи менеджера за день: сначала активные по времени, в конце отменённые.
  Отдельного эндпоинта для отменённых встреч в ТЗ нет, поэтому они видны здесь.
- **Менеджеры** заводятся миграцией: эндпоинта для них в ТЗ нет.
- **БИН** — 12 цифр, уникален; телефон — 10–15 цифр с необязательным `+`.
- **Пагинация** — `page` с нуля, `size` по умолчанию 20 (максимум 100), сортировка по id.

## Структура

```
src/main/java/com/callscheduler
├── company   организации: создание, список, статус
├── call      звонки и правило meeting_set
├── meeting   встречи: назначение, перенос, отмена, расписание, рабочие часы
├── manager   менеджеры
└── error     исключения и их преобразование в HTTP-ответы
```
