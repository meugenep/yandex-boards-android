# Яндекс Доски — Android APK

## Сборка APK

### Требования
- **JDK 17+** (для Android 16/17)
- **Android SDK** (API 36 / Android 16 target)
- **Gradle 8.10.1+**

### Команды сборки

```bash
cd android
.\gradlew.bat assembleRelease
```

APK будет в:
```
android\app\build\outputs\apk\release\app-release.apk
```

### Дебаг APK

```bash
cd android
.\gradlew.bat assembleDebug
```

## Возможности

### Ориентации
- ✅ **Портрет** (▯)
- ✅ **Альбом** (▭)
- ✅ **Полная поддержка** всех углов через `fullSensor`

### Тройные раскладушки
- ✅ **Multi-window** (`resizeableActivity="true"`)
- ✅ **Split-screen** (large/small/multiple)
- ✅ **Multi-display** support
- ✅ **Max fold count**: 3+

### Android 16/17 (API 36)
- ✅ `compileSdk 36`
- ✅ `targetSdk 36`
- ✅ `Java 17`
- ✅ Material Design 3

## Структура

```
yandex-boards-app/
├── www/                    # Веб-код (HTML/CSS/JS)
│   └── index.html
├── android/                # Android проект
│   ├── app/
│   │   ├── build.gradle    # Конфигурация сборки
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/yandex/boards/MainActivity.java
│   │       └── assets/www/index.html
│   ├── gradle/wrapper/
│   ├── build.gradle
│   └── settings.gradle
└── capacitor.config.json
```

## OAuth

Используется **PKCE flow** (Authorization Code with PKCE):
1. Генерация code_verifier и code_challenge на клиенте
2. Открытие OAuth в Chrome (системный браузер)
3. Получение code через deep link
4. Обмен code + verifier на access_token

**Client ID:** `e44bb58e1b5b4577bec9ff2abb6dad3d`
**Redirect URI:** `yandexboards://callback`

## Whiteboard

URL: `https://boards.yandex.ru/whiteboard/?hash=e0469e6c2c6901788c0bd174fc2a7570`

## Multi-window поддержка

### Triple-fold устройства
- Поддержка **до 3 складок** одновременно
- Корректное отображение через `DisplayCutout`
- `max_aspect = 2.4` для широких экранов

### Split-screen
- Large container: полная версия
- Small container: адаптивная версия

### Freeform window
- Поддержка **перетаскивания** и **изменения размера**
- Пересчёт layout при resize
