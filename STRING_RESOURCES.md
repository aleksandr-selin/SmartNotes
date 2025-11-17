# Строковые ресурсы в SmartNotes

## Структура

Все строки приложения хранятся в XML файле:

```
composeApp/src/commonMain/composeResources/values/strings.xml
```

## Использование

### В Composable функциях

```kotlin
import org.jetbrains.compose.resources.stringResource
import smartnotes.composeapp.generated.resources.Res
import smartnotes.composeapp.generated.resources.*

@Composable
fun MyScreen() {
    // Простая строка
    Text(stringResource(Res.string.notes_title))

    // Строка с параметром
    Text(stringResource(Res.string.error_format, "Ошибка загрузки"))

    // Строка с числами
    Text(stringResource(Res.string.subtasks_count_format, 3, 5))
}
```

## Добавление новых строк

1. Откройте `strings.xml`
2. Добавьте новую строку:
   ```xml
   <string name="my_new_string">Моя новая строка</string>
   ```
3. Пересоберите проект (Gradle автоматически сгенерирует константы)
4. Используйте:
   ```kotlin
   Text(stringResource(Res.string.my_new_string))
   ```

## Локализация

Для добавления других языков создайте папки с переводами:

```
composeResources/
├── values/              # Русский (по умолчанию)
│   └── strings.xml
├── values-en/           # Английский
│   └── strings.xml
└── values-kk/           # Казахский
    └── strings.xml
```

