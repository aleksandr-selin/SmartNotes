# Presentation Layer - Руководство

## 🎯 Обзор

Presentation слой следует архитектуре **MVVM** с использованием:
- **Voyager** для навигации
- **ScreenModel** для ViewModels
- **Koin** для Dependency Injection
- **Compose Multiplatform** для UI

## 🏗️ Архитектура

### Общая структура

```
presentation/
├── navigation/
│   ├── RootScreen.kt          # Корневой экран с TabNavigator
│   └── tabs/
│       ├── NotesTab.kt        # Таб заметок
│       └── TasksTab.kt        # Таб задач
│
└── screens/
    ├── notes/
    │   ├── NotesListScreen.kt
    │   ├── NotesListViewModel.kt
    │   ├── NoteDetailScreen.kt
    │   └── NoteDetailViewModel.kt
    │
    └── tasks/
        ├── TasksListScreen.kt
        ├── TasksListViewModel.kt
        ├── TaskDetailScreen.kt
        └── TaskDetailViewModel.kt
```

### Навигация

```
App → RootScreen (TabNavigator)
├── NotesTab → Navigator (StackNavigator)
│   ├── NotesListScreen
│   └── NoteDetailScreen(noteId?)
│
└── TasksTab → Navigator (StackNavigator)
    ├── TasksListScreen
    └── TaskDetailScreen(taskId?)
```

## 📱 Создание нового экрана

### 1. Создайте ViewModel (ScreenModel)

```kotlin
class MyScreenViewModel(
    private val myUseCase: MyUseCase
) : ScreenModel {
    
    // UI State
    private val _uiState = MutableStateFlow<MyUiState>(MyUiState.Loading)
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
    
    // Прочие состояния
    private val _data = MutableStateFlow("")
    val data: StateFlow<String> = _data.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        screenModelScope.launch {
            _uiState.value = MyUiState.Loading
            myUseCase().fold(
                onSuccess = { result ->
                    _uiState.value = MyUiState.Success(result)
                },
                onFailure = { error ->
                    _uiState.value = MyUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
    
    fun updateData(newData: String) {
        _data.value = newData
    }
}

// UI State
sealed class MyUiState {
    data object Loading : MyUiState()
    data object Empty : MyUiState()
    data class Success(val data: MyData) : MyUiState()
    data class Error(val message: String) : MyUiState()
}
```

### 2. Зарегистрируйте в Koin

```kotlin
// di/PresentationModule.kt
val presentationModule = module {
    // Без параметров
    factoryOf(::MyScreenViewModel)
    
    // С параметрами
    factory { params ->
        MyScreenViewModel(
            id = params.getOrNull(),
            myUseCase = get()
        )
    }
}
```

### 3. Создайте Screen

```kotlin
data class MyScreen(val id: Long?) : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<MyScreenViewModel> {
            parametersOf(id)
        }
        val uiState by viewModel.uiState.collectAsState()
        val data by viewModel.data.collectAsState()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Screen") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            when (val state = uiState) {
                is MyUiState.Loading -> LoadingContent(paddingValues)
                is MyUiState.Empty -> EmptyContent(paddingValues)
                is MyUiState.Success -> SuccessContent(state.data, paddingValues)
                is MyUiState.Error -> ErrorContent(state.message, paddingValues)
            }
        }
    }
}
```

## 🎨 Лучшие практики

### UI State Pattern

Всегда используйте sealed class для UI состояний:

```kotlin
sealed class UiState {
    data object Loading : UiState()
    data object Empty : UiState()
    data class Success(val data: T) : UiState()
    data class Error(val message: String) : UiState()
}
```

**Почему:**
- ✅ Типобезопасность
- ✅ Exhaustive when expressions
- ✅ Явное управление состояниями
- ✅ Легко тестировать

### Навигация

**Передача параметров:**

```kotlin
// Определение Screen с параметром
data class DetailScreen(val itemId: Long) : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<DetailViewModel> {
            parametersOf(itemId)
        }
    }
}

// Навигация
navigator.push(DetailScreen(itemId = 123))
```

**Возврат назад:**

```kotlin
// Простой возврат
navigator.pop()

// Возврат с результатом (через ViewModel/StateHolder)
viewModel.saveData {
    navigator.pop()
}
```

### StateFlow vs State

**Используйте StateFlow в ViewModel:**

```kotlin
private val _data = MutableStateFlow("")
val data: StateFlow<String> = _data.asStateFlow()
```

**Собирайте в Composable:**

```kotlin
val data by viewModel.data.collectAsState()
```

**Почему:**
- ✅ Реактивность
- ✅ Автоматическая отписка при уничтожении Composable
- ✅ Thread-safe обновления

### Валидация форм

Создавайте отдельный StateFlow для состояния кнопок:

```kotlin
private val _canSave = MutableStateFlow(false)
val canSave: StateFlow<Boolean> = _canSave.asStateFlow()

fun updateTitle(newTitle: String) {
    _title.value = newTitle
    updateValidation()
}

private fun updateValidation() {
    _canSave.value = _title.value.isNotBlank() && _content.value.isNotBlank()
}
```

В UI:

```kotlin
val canSave by viewModel.canSave.collectAsState()

Button(
    onClick = { viewModel.save() },
    enabled = canSave
) {
    Text("Save")
}
```

### Обработка ошибок

**В ViewModel:**

```kotlin
myUseCase().fold(
    onSuccess = { result ->
        _uiState.value = UiState.Success(result)
    },
    onFailure = { error ->
        _uiState.value = UiState.Error(error.message ?: "Unknown error")
    }
)
```

**В UI:**

```kotlin
is UiState.Error -> {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.message,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = { viewModel.retry() }) {
            Text("Retry")
        }
    }
}
```

## 🔄 Жизненный цикл

### ScreenModel

- **Создание:** При первом отображении Screen
- **Активность:** Пока Screen в стеке навигации
- **Уничтожение:** Когда Screen удаляется из стека

### Coroutine Scopes

```kotlin
// Используйте screenModelScope
screenModelScope.launch {
    // Автоматически отменяется при уничтожении ScreenModel
}
```

### Flow Subscriptions

```kotlin
// В ViewModel
flow.collect { data ->
    _uiState.value = UiState.Success(data)
}

// В Composable
val data by viewModel.data.collectAsState()
// Автоматическая подписка/отписка
```

## 🧪 Тестирование

### Unit тесты ViewModel

```kotlin
class MyViewModelTest {
    private lateinit var viewModel: MyViewModel
    private val mockUseCase: MyUseCase = mockk()
    
    @Before
    fun setup() {
        viewModel = MyViewModel(mockUseCase)
    }
    
    @Test
    fun `loadData should update uiState to Success`() = runTest {
        // Given
        val expectedData = MyData(...)
        coEvery { mockUseCase() } returns Result.success(expectedData)
        
        // When
        viewModel.loadData()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is MyUiState.Success)
        assertEquals(expectedData, (state as MyUiState.Success).data)
    }
}
```

## 📊 Текущий статус

**Реализованные экраны:**
- ✅ NotesListScreen - список заметок
- ✅ NoteDetailScreen - создание/редактирование заметки
- ✅ TasksListScreen - список задач с фильтрами
- ✅ TaskDetailScreen - создание/редактирование задачи

**Основной функционал:**
- ✅ Tab Navigation (Заметки / Задачи)
- ✅ Stack Navigation внутри табов
- ✅ CRUD операции для заметок и задач
- ✅ Управление подзадачами
- ✅ Фильтры задач (All, Today, Active, Completed)
- ✅ Диалоги подтверждения
- ✅ Валидация форм

## 🚀 Рекомендации для расширения

### Новые экраны

1. Следуйте паттерну: ViewModel + Screen + UiState
2. Регистрируйте в PresentationModule
3. Используйте Voyager для навигации
4. Собирайте StateFlow через collectAsState()

### Сложная навигация

Для вложенной навигации используйте:

```kotlin
TabNavigator(tab = HomeTab) {
    CurrentTab()
}

// Или Stack в Stack
Navigator(screen = ListScreen) {
    CurrentScreen()
}
```

### Shared State

Если нужен shared state между экранами:

```kotlin
// SharedViewModel как singleton
single { SharedViewModel() }

// Или через Voyager ScreenModel с SharedScope
```

## 📚 Полезные ресурсы

- [Voyager Documentation](https://voyager.adriel.cafe/)
- [Koin Documentation](https://insert-koin.io/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

---

Следуя этим принципам, вы можете легко расширять Presentation слой новыми экранами и функциями! 🎉
