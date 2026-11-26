# Shizuku Toolbox

Android-приложение, которое через **Shizuku** выполняет системные команды от имени
`shell` (uid 2000) — то же самое, что вы делаете через `adb shell`, только с телефона
и в симпатичном интерфейсе.

## Что умеет

**Главная**
- Wi-Fi, мобильные данные, Bluetooth, авиарежим — переключатели с реальным состоянием.
- Перезагрузка, recovery, выключение, перезапуск SystemUI.
- Скриншот и запись экрана в Pictures/Movies.
- `am kill-all`, очистка кэша (`pm trim-caches`), Doze force-idle/unforce.
- Отключение и возврат анимаций.
- Сброс `wm size` / `wm density`.
- Выдача приложению `WRITE_SECURE_SETTINGS`.

**Приложения**
- Список всех пакетов с поиском, фильтром системных и пометкой «заморожено».
- Остановить, заморозить/разморозить (`pm disable-user` / `pm enable`).
- Очистить данные, удалить для пользователя (`pm uninstall --user 0`).
- Сохранить APK в `Download`.
- Запрет/разрешение автозапуска (`appops BOOT_COMPLETED`).
- Ограничение фоновой работы (`am set-standby-bucket restricted`).
- `dumpsys package` по пакету.

**Терминал**
- Любая shell-команда, потоковый вывод, история и готовые пресеты.

**Ещё**
- Статус Shizuku, диагностика (батарея, память, диск, сеть, нагрузка, устройство),
  инструкция по запуску Shizuku.

## Как собрать APK

### Вариант 1: GitHub Actions (собрать можно прямо с телефона)

1. Создай пустой репозиторий на GitHub.
2. Загрузи туда содержимое этой папки (можно через веб-интерфейс GitHub:
   «Add file → Upload files»).
3. Открой вкладку **Actions** → workflow **Build APK** → **Run workflow**.
4. Через пару минут открой завершившийся запуск и скачай артефакт
   `ShizukuToolbox-debug` — внутри `app-debug.apk`.

### Вариант 2: Android Studio

Открой папку проекта, дождись Gradle Sync и нажми **Run**.
Нужен JDK 17 и Android SDK 35.

### Вариант 3: командная строка на компьютере

```bash
gradle :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
JDK 17, Android SDK 35, Gradle 8.9+.

## Настройка Shizuku

1. Установи [Shizuku](https://shizuku.rikka.app/) (Google Play / GitHub).
2. Включи «Беспроводную отладку» в настройках разработчика.
3. В Shizuku нажми «Запустить» → «Беспроводная отладка» → подтверди.
4. Открой Shizuku Toolbox и выдай разрешение.

Если Shizuku запущен через root — шаг с отладкой не нужен.

## Структура

```
app/src/main/java/com/narezany/shizukutoolbox/
  core/ShizukuManager.kt   — binder, разрешение, запросы
  core/Shell.kt            — запуск команд через Shizuku.newProcess
  core/Ui.kt               — всплывающие сообщения
  data/AppsRepo.kt         — список пакетов
  data/SystemRepo.kt       — состояние переключателей
  ui/                      — Compose-экраны
```

## Осторожно

Всё выполняется от `shell`. Удаление, очистка данных, заморозка системных пакетов и
перезагрузка — необратимые действия. Не замораживай пакеты, без которых не грузится
оболочка (SystemUI, launcher, telephony).

Приложение не связано с RikkaApps, это независимый клиент.
