# Presentation Layer - Реализация

## 🎯 Обзор

Presentation слой реализован с использованием **Voyager**
для навигации и **Koin** для Dependency Injection.
Следует архитектуре **MVVM** с использованием **ScreenModel** от Voyager.

## 🧭 Навигация с Voyager

### Архитектура навигации

```
App
│
└── RootScreen (TabNavigator)
    │
    ├── NotesTab
    │   └── Navigator (StackNavigator)
    │       ├── NotesListScreen
    │       └── NoteDetailScreen (noteId: Long?)
    │
    └── TasksTab
        └── Navigator (StackNavigator)
            ├── TasksListScreen
            │   - Фильтры: All, Today, Active, Completed
            └── TaskDetailScreen (taskId: Long?)
                - С управлением подзадачами
```

### Особенности навигации

1. **TabNavigator** - переключение между Notes и Tasks через нижнее меню
2. **StackNavigator** - навигация в пределах каждого таба (список → детали)
3. **Параметры** - передача ID через `data class` Screen
4. **Deep navigation** - независимый стек для каждого таба

---

## 📱 ViewModels (ScreenModels)

### NotesListViewModel

**Состояния:**

```kotlin
sealed class NotesListUiState {
    data object Loading
    data object Empty
    data class Success(val notes: List<Note>)
    data class Error(val message: String)
}
```

**Основные функции:**

- `loadNotes(sortByCreatedAt: Boolean)` - загрузка заметок
- `searchNotes(query: String)` - поиск по заголовку и содержимому
- `deleteNote(noteId: Long)` - удаление заметки
- `clearSearch()` - очистка поиска

**Scope:** `factory` (создаётся для каждого экрана)

---

### NoteDetailViewModel

**Состояния:**

```kotlin
sealed class NoteDetailUiState {
    data object Loading
    data object Success
    data class Error(val message: String)
}
```

**Основные функции:**

- `updateTitle(newTitle: String)` - обновление заголовка
- `updateContent(newContent: String)` - обновление содержимого
- `saveNote(onSuccess: () -> Unit)` - сохранение (create/update)
- `canSave(): Boolean` - валидация перед сохранением

**Параметры:** `noteId: Long?` (null для создания)

---

### TasksListViewModel

**Состояния:**

```kotlin
sealed class TasksListUiState {
    data object Loading
    data class Empty(val filter: TasksFilter)
    data class Success(val tasks: List<Task>, val filter: TasksFilter)
    data class Error(val message: String)
}

enum class TasksFilter {
    ALL, TODAY, ACTIVE, COMPLETED
}
```

**Основные функции:**

- `loadTasks(filter: TasksFilter)` - загрузка с фильтром
- `toggleTaskCompletion(taskId: Long, isCompleted: Boolean)` - переключение статуса
- `deleteTask(taskId: Long)` - удаление задачи
- `completeAndDeleteTask(taskId: Long)` - завершение с удалением

**Особенности:**

- Фильтры: All, Today, Active, Completed
- Автоматическое обновление через Flow

---

### TaskDetailViewModel

**Состояния:**

```kotlin
sealed class TaskDetailUiState {
    data object Loading
    data object Success
    data class Error(val message: String)
}
```

**Основные функции:**

- `updateTitle/Description/Importance()` - обновление полей
- `toggleIsToday()` - переключение флага "Сегодня"
- `addSubtask(title: String)` - добавление подзадачи
- `toggleSubtask(subtaskId: Long)` - переключение статуса подзадачи
- `deleteSubtask(subtaskId: Long)` - удаление подзадачи
- `saveTask(onSuccess: () -> Unit)` - сохранение задачи

**Параметры:** `taskId: Long?` (null для создания)

**Особенности:**

- Управление подзадачами
- Три уровня важности (Low, Medium, High)
- Флаг "Сделать сегодня"

---

## 🎨 UI Компоненты

### NotesListScreen

**Функциональность:**

- Отображение списка заметок
- FloatingActionButton для создания
- Клик на заметку → NoteDetailScreen

**UI элементы:**

- TopAppBar с заголовком
- LazyColumn со списком заметок
- NoteCard - карточка заметки
- Состояния: Loading, Empty, Success, Error

---

### NoteDetailScreen

**Функциональность:**

- Создание новой заметки (noteId = null)
- Редактирование существующей (noteId != null)
- Валидация: title и content не пустые

**UI элементы:**

- TopAppBar с кнопками "Назад" и "Сохранить"
- OutlinedTextField для заголовка
- OutlinedTextField для содержимого (multiline)
- LinearProgressIndicator при сохранении

---

### TasksListScreen

**Функциональность:**

- Отображение списка задач с фильтрами
- FilterChips для переключения фильтров
- Checkbox для быстрого завершения
- FloatingActionButton для создания

**UI элементы:**

- TopAppBar
- Row с FilterChips (All, Today, Active, Completed)
- LazyColumn со списком задач
- TaskCard - карточка задачи с:
    - Checkbox для завершения
    - Заголовок и описание
    - Прогресс подзадач
    - Badge важности (для Medium/High)

---

### TaskDetailScreen

**Функциональность:**

- Создание/редактирование задачи
- Управление подзадачами
- Выбор важности (Low, Medium, High)
- Переключение флага "Сегодня"

**UI элементы:**

- TopAppBar с кнопками
- OutlinedTextField для заголовка и описания
- FilterChips для важности
- Switch для флага "Сегодня"
- Список подзадач с возможностью:
    - Добавления (через диалог)
    - Переключения статуса (Checkbox)
    - Удаления (IconButton)

---

## 🔌 Koin Integration

### PresentationModule

```kotlin
val presentationModule = module {
    // Notes
    factoryOf(::NotesListViewModel)
    factory { params ->
        NoteDetailViewModel(
            noteId = params.getOrNull(),
            getNoteByIdUseCase = get(),
            addNoteUseCase = get(),
            updateNoteUseCase = get()
        )
    }

    // Tasks
    factoryOf(::TasksListViewModel)
    factory { params ->
        TaskDetailViewModel(
            taskId = params.getOrNull(),
            // ... use cases
        )
    }
}
```

### Использование в Screens

```kotlin
// Без параметров
val viewModel = getScreenModel<NotesListViewModel>()

// С параметрами
val viewModel = getScreenModel<NoteDetailViewModel> {
    parametersOf(noteId)
}
```

---

## 🔄 Жизненный цикл

### ScreenModel Lifecycle

1. **Создание** - при первом отображении Screen
2. **Активность** - пока Screen в стеке навигации
3. **Уничтожение** - когда Screen удаляется из стека

### Flow Subscription

- `screenModelScope.launch` - автоматически отменяется при уничтожении
- `collectAsState()` - автоматическая подписка/отписка

---

## 📊 Состояния UI

### Общая стратегия

Все ViewModels используют паттерн **UI State**:

```kotlin
sealed class UiState {
    data object Loading      // Загрузка данных
    data object Empty        // Нет данных
    data class Success(...)  // Успех с данными
    data class Error(...)    // Ошибка с сообщением
}
```

### Преимущества:

- ✅ Типобезопасность
- ✅ Exhaustive when
- ✅ Явное управление состояниями
- ✅ Легко тестировать

---

## 🎯 Ключевые решения

### 1. Voyager вместо Jetpack Navigation

**Почему:**

- ✅ KMP-native
- ✅ Простой API
- ✅ Интеграция с Koin
- ✅ Tab Navigation из коробки

### 2. ScreenModel вместо ViewModel

**Почему:**

- ✅ Работает на всех платформах (KMP)
- ✅ Интеграция с Voyager
- ✅ Похож на Jetpack ViewModel

### 3. Factory scope для ViewModels

**Почему:**

- ✅ Каждый Screen получает новый экземпляр
- ✅ Lifecycle управляется Voyager
- ✅ Не нужно manually dispose

### 4. Параметры через data class Screen

**Почему:**

- ✅ Типобезопасность
- ✅ Сериализация для state restoration
- ✅ Чёткий контракт Screen

---

## 🚀 Следующие шаги

### Расширения UI:

1. **Анимации переходов** - Voyager Transitions
2. **Swipe actions** - для удаления в списках
3. **Pull-to-refresh** - обновление списков
4. **Search bar** - полноценный поиск в NotesListScreen
5. **Empty states** - более красивые пустые состояния
6. **Error handling** - Snackbar для ошибок

### Оптимизации:

1. **Pagination** - для больших списков
2. **Image caching** - если добавим изображения
3. **Offline-first** - уже реализовано через SQLDelight

### Тестирование:

1. **ViewModel tests** - Unit тесты с MockK
2. **Screen tests** - UI тесты с Compose Testing
3. **Navigation tests** - тесты навигации

---

## 📝 Итоги

**Реализовано:**

- ✅ Полная навигация с Voyager (Tab + Stack)
- ✅ 4 ScreenModels с управлением состоянием
- ✅ 4 полнофункциональных Screen
- ✅ Интеграция с Koin DI
- ✅ UI состояния для всех экранов
- ✅ CRUD операции для Notes и Tasks
- ✅ Управление подзадачами
- ✅ Фильтры для задач
- ✅ Валидация форм

**Архитектура:**

- 🏗️ Clean Architecture
- 📐 MVVM pattern
- 🔄 Unidirectional Data Flow
- 🎯 Single Source of Truth
- ⚡ Reactive UI с Flow

Presentation слой полностью готов к использованию! 🎉

