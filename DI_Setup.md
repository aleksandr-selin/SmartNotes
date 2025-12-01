# Настройка Dependency Injection с Koin

## Обзор

В проекте используется **Koin** для Dependency Injection. Koin — это легковесный DI-фреймворк для Kotlin, который отлично работает с Kotlin Multiplatform.

## Структура модулей

### 1. **dataModule** (`DataModule.kt`)
Модуль для Data слоя, содержит:
- `NotesDatabase` — SQLDelight база данных (singleton)
- `NotesRepository` — репозиторий для заметок
- `TasksRepository` — репозиторий для задач и подзадач

**Примечание:** `DatabaseDriverFactory` предоставляется platform-specific модулями.

### 2. **domainModule** (`DomainModule.kt`)
Модуль для Domain слоя, содержит все Use Cases:
- **Notes:** AddNote, GetAllNotes, GetNoteById, UpdateNote, DeleteNote, SearchNotes
- **Tasks:** AddTask, GetAllTasks, GetTaskById, UpdateTask, CompleteTask, DeleteTask, GetTasksForToday
- **Subtasks:** AddSubtask, ToggleSubtask, DeleteSubtask

**Scope:** `factory` — Use Cases создаются заново при каждом вызове, так как не хранят состояние.

### 3. **presentationModule** (`PresentationModule.kt`)
Модуль для Presentation слоя, содержит ScreenModels:
- `NotesListViewModel` - управление списком заметок
- `NoteDetailViewModel` - создание/редактирование заметки (с параметром `noteId`)
- `TasksListViewModel` - управление списком задач с фильтрами
- `TaskDetailViewModel` - создание/редактирование задачи (с параметром `taskId`)

**Scope:** `factory` — ViewModels создаются для каждого экрана и управляются Voyager.

### 4. **androidModule** (`AndroidModule.kt`)
Android-специфичный модуль, содержит:
- `DatabaseDriverFactory(context)` — драйвер SQLDelight для Android

### 5. **iosModule** (`IosModule.kt`)
iOS-специфичный модуль, содержит:
- `DatabaseDriverFactory()` — драйвер SQLDelight для iOS

## Инициализация Koin

### Общая схема

```kotlin
// KoinInitializer.kt (commonMain)
fun initializeKoin(platformModule: Module) {
    startKoin {
        modules(
            dataModule,
            domainModule,
            presentationModule,
            platformModule  // Android или iOS специфичный
        )
    }
}
```

### Android

Инициализация происходит автоматически в `SmartNotesApplication` (Application класс):

```kotlin
class SmartNotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlatformKoinInitializer.initialize(this)
    }
}
```

**AndroidManifest.xml:**
```xml
<application
    android:name=".SmartNotesApplication"
    ...>
```

### iOS

Инициализация происходит **автоматически** при первом обращении к Koin:

```kotlin
// PlatformKoinInitializer.ios.kt
actual object PlatformKoinInitializer {
    init {
        doInitialize()
    }
}
```

Swift код не требует изменений - Koin инициализируется автоматически при первом использовании DI.

## Использование Koin

### В ScreenModels с Voyager

```kotlin
import cafe.adriel.voyager.koin.getScreenModel
import org.koin.core.parameter.parametersOf

// Без параметров
@Composable
fun Content() {
    val viewModel = getScreenModel<NotesListViewModel>()
    // ...
}

// С параметрами
@Composable
fun Content() {
    val viewModel = getScreenModel<NoteDetailViewModel> {
        parametersOf(noteId)
    }
    // ...
}
```

### В обычном Compose (без Voyager)

```kotlin
import org.koin.compose.koinInject

@Composable
fun MyScreen() {
    val repository: NotesRepository = koinInject()
    // ...
}
```

### Ручная инъекция (если нужно)

```kotlin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MyClass : KoinComponent {
    private val repository: NotesRepository by inject()
}
```

## Зависимости в Gradle

**libs.versions.toml:**
```toml
[versions]
koin = "4.1.0"

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }
```

**build.gradle.kts (composeApp):**
```kotlin
// iOS фреймворк конфигурация
listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
        baseName = "ComposeApp"
        isStatic = true
        export(libs.koin.core)
        
        // ВАЖНО: Линковка с системным SQLite для iOS
        linkerOpts("-lsqlite3")
    }
}

// Зависимости
commonMain.dependencies {
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
}

androidMain.dependencies {
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}

iosMain.dependencies {
    api(libs.koin.core)  // api() для экспорта в фреймворк
}
```

## Почему Koin?

### Плюсы:
1. **Легковесный** — не требует кодогенерации
2. **DSL на Kotlin** — удобный и читаемый синтаксис
3. **KMP-совместимый** — работает на всех платформах
4. **Простая отладка** — понятные сообщения об ошибках
5. **Нет Reflection на iOS** — использует обычные конструкторы

### Минусы:
1. **Runtime проверка зависимостей** — ошибки обнаруживаются при запуске
2. **Меньше compile-time безопасности** по сравнению с Dagger/Hilt

## Альтернативы

1. **Kotlin-inject** 
   - Compile-time DI через KSP
   - Плюсы: Безопасность на этапе компиляции
   - Минусы: Более сложная настройка, требует KSP

2. **Kodein-DI**
   - Похож на Koin
   - Плюсы: Больше типобезопасности
   - Минусы: Меньше популярности и поддержки

3. **Manual DI (без фреймворка)**
   - Плюсы: Полный контроль, нет зависимостей
   - Минусы: Больше boilerplate кода

## Следующие шаги

Текущий статус DI:
1. ✅ Data Layer полностью покрыт DI
2. ✅ Domain Layer полностью покрыт DI (все Use Cases)
3. ✅ Presentation Layer полностью покрыт DI (все ScreenModels)
4. ✅ Platform-specific модули для Android и iOS
5. ✅ Voyager интеграция для навигации

DI настройка завершена и готова к использованию! 🎉