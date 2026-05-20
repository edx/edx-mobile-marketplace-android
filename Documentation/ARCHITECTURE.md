# EDX Mobile Marketplace - Android Architecture Guide

**Last Updated:** April 2026
**Framework:** Koin DI + Clean Architecture (3-Layer)
**Language:** Kotlin
**Min API:** Android 24 (API 24)

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Architecture Layers](#core-architecture-layers)
3. [Dependency Injection Pattern](#dependency-injection-pattern)
4. [State Management](#state-management)
5. [Module Structure](#module-structure)
6. [Design Patterns](#design-patterns)
7. [Tech Stack](#tech-stack)
8. [Best Practices](#best-practices)
9. [Common Patterns by Feature](#common-patterns-by-feature)
10. [Anti-Patterns to Avoid](#anti-patterns-to-avoid)

---

## Architecture Overview

```
┌─────────────────────────────────────────────┐
│  PRESENTATION LAYER                         │
│  (Fragments, ViewModels, Compose, XML UI)   │
└─────────────┬───────────────────────────────┘
              │ (depends on)
┌─────────────▼───────────────────────────────┐
│  DOMAIN LAYER                               │
│  (Interactors/UseCases, Business Logic)     │
└─────────────┬───────────────────────────────┘
              │ (depends on)
┌─────────────▼───────────────────────────────┐
│  DATA LAYER                                 │
│  (Repositories, Database, API Calls)        │
└─────────────────────────────────────────────┘
```

**Key Rule:** Lower layers have NO knowledge of higher layers. This prevents circular dependencies and maintains clean architecture:

```
❌ WRONG:
Data → Domain ← Presentation
            ↑
            └─ Data (creates cycle)

✅ CORRECT:
Data ← Domain ← Presentation
         (one-way dependency)
```

---

## Core Architecture Layers

### 1. Presentation Layer

**Responsibility:** Display data to user, handle user input, manage UI state

**Components:**
- **Fragments** - UI containers, lifecycle management
- **ViewModels** - State management, business logic orchestration
- **Composables** - Modern UI with Jetpack Compose
- **Adapters** - RecyclerView adapters for lists
- **Routers** - Navigation between screens/modules

**State Management:** StateFlow for main UI state, SharedFlow for transient events

**Example Structure:**
```
course/
├── presentation/
│   ├── unit/
│   │   ├── video/
│   │   │   ├── VideoUnitFragment.kt
│   │   │   ├── VideoUnitViewModel.kt
│   │   │   ├── EncodedVideoUnitViewModel.kt
│   │   │   └── YoutubeVideoUnitFragment.kt
│   │   ├── html/
│   │   │   ├── HtmlUnitFragment.kt
│   │   │   └── HtmlUnitViewModel.kt
│   ├── outline/
│   │   ├── CourseOutlineFragment.kt
│   │   └── CourseOutlineViewModel.kt
│   ├── CourseAnalytics.kt
│   └── CourseRouter.kt (navigation interface)
```

**Key Rules:**
- All ViewModels inherit from `BaseViewModel`
- State exposed as readonly `StateFlow<UIState>` (sealed class)
- Never expose mutable state to UI
- Analytics injected into every ViewModel
- All screen navigation through Router interface

### 2. Domain Layer

**Responsibility:** Encapsulate business logic, keep it UI-independent and framework-agnostic

**Components:**
- **Interactors/UseCases** - Orchestrate repos and domain models
- **Domain Models** - Pure data classes representing business entities
- **Repository Interfaces** - Contracts for data access

**Example Structure:**
```
course/
├── domain/
│   ├── interactor/
│   │   ├── CourseInteractor.kt
│   │   └── DownloadInteractor.kt
│   └── model/
│       ├── CourseStructure.kt
│       ├── EnrolledCourse.kt
│       └── Block.kt
```

**Key Rules:**
- No Android dependencies (no Context, Activity, Fragment)
- No UI framework imports (no Compose, no Jetpack)
- Pure Kotlin, easily testable
- Interactors use `suspend` functions (coroutines)
- Return types are `Flow<T>` for streams or `T` for single values

**Example Interactor:**
```kotlin
class CourseInteractor(
    private val repository: CourseRepository
) {
    // Streaming API - emits multiple times
    suspend fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = true,
    ): Flow<CourseStructure?> {
        return repository.getCourseStructureFlow(courseId, forceRefresh)
    }

    // One-shot API
    suspend fun getCourseStructure(
        courseId: String,
        isNeedRefresh: Boolean = false
    ): CourseStructure {
        return repository.getCourseStructure(courseId, isNeedRefresh)
    }
}
```

### 3. Data Layer

**Responsibility:** Manage data from all sources (API, Database, Cache)

**Components:**
- **Repositories** - Concrete implementations of repository interfaces
- **API Clients** - Retrofit services for REST calls
- **DAO (Database Access)** - Room database operations
- **Models** - Data classes matching API responses

**Example Structure:**
```
course/
├── data/
│   ├── repository/
│   │   └── CourseRepository.kt
│   ├── api/
│   │   └── CourseApi.kt
│   ├── database/
│   │   ├── dao/
│   │   │   └── CourseDao.kt
│   │   └── model/
│   │       └── CourseTable.kt
│   └── storage/
│       └── CoursePreferences.kt
```

**Key Rules:**
- Repository is the single source of truth
- Implement three-level caching: Memory → Database → API
- Flow returns multiple emissions: cached → fresh
- Never expose DB/API models to higher layers
- Convert between data models and domain models

**3-Level Caching Example:**
```kotlin
class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val preferences: CorePreferences,
    private val networkConnection: NetworkConnection,
) {
    // In-memory cache
    private val courseStructure = mutableMapOf<String, CourseStructure>()

    suspend fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = true,
    ): Flow<CourseStructure> = channelFlowWithAwait {
        // Step 1: Check memory cache
        courseStructure[courseId]?.let { send(it) }

        // Step 2: Check database
        courseDao.getCourse(courseId)?.let { send(it.toDomain()) }

        // Step 3: Fetch from API if online and refresh needed
        if (networkConnection.isOnline() && forceRefresh) {
            val fresh = api.getCourse(courseId)
            courseDao.insert(fresh.toTable())
            courseStructure[courseId] = fresh.toDomain()
            send(fresh.toDomain())
        }
    }
}
```

---

## Dependency Injection Pattern

### Framework: Koin

**Why Koin?**
- Lightweight, Kotlin-native
- No compile-time processing needed
- Easy to test (swap implementations)
- Supports lazy loading of feature modules
- Feature flags compatible

### DI Architecture

**File Structure:**
```
app/src/main/java/org/openedx/app/di/
├── AppModule.kt          # Core app dependencies
├── ScreenModule.kt       # All ViewModel definitions
└── NetworkingModule.kt   # Network configuration
```

**Multi-Module Support:**
```
Each feature module has a ModuleProvider:

core/src/main/java/org/openedx/core/di/
└── KoinModuleProvider.kt (interface)

feature_module/src/main/java/org/openedx/feature/di/
└── FeatureModuleProvider.kt (implementation)
```

### Dependency Definition Patterns

#### 1. Singleton (single instance, app lifetime)
```kotlin
single { PreferencesManager(get()) }
single { NetworkConnection(get()) }
single { ResourceManager(get()) }
```

**Use for:**
- Managers (shared state across app)
- Repositories (single source of truth)
- Network clients
- Database
- Notifiers

#### 2. Factory (new instance every time)
```kotlin
factory { AuthRepository(get(), get(), get(), get()) }
factory { CourseInteractor(get()) }
factory { AgreementProvider(get(), get()) }
```

**Use for:**
- Interactors (should be recreated per use)
- One-off processors
- Lightweight objects

#### 3. ViewModel (lifecycle-aware, scoped to Fragment/Activity)
```kotlin
viewModel { (courseId: String) ->
    CourseOutlineViewModel(
        courseId,
        courseTitle,
        get(),  // Inject dependencies via get()
        get(),
        get(),  // Can reference any singleton
        // ... more deps
    )
}
```

**Use for:**
- Fragment ViewModels
- Activity ViewModels
- Screen-level state

#### 4. Named Qualifiers (multiple instances of same type)
```kotlin
single(named("IODispatcher")) {
    Dispatchers.IO
}

// Usage
viewModel {
    SomeViewModel(
        dispatcher = get(named("IODispatcher")),
        // ... other deps
    )
}
```

### Initialization

**Application Class:**
```kotlin
class OpenEdXApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Load core modules
        startKoin {
            androidContext(this@OpenEdXApp)
            modules(
                appModule,
                screenModule,
                networkingModule,
                // ... other modules
            )
        }

        // Lazy load feature modules based on feature flags
        val optionalModules = listOfNotNull(
            if (featureManager.isEnabled("BETA_FEATURES"))
                BetaFeatureModuleProvider().getModules()
            else null
        ).flatten()

        loadKoinModules(optionalModules)
    }
}
```

### Dependency Resolution in Fragments

**ViewModel Injection:**
```kotlin
class CourseOutlineFragment : Fragment() {
    // Get ViewModel with parameters
    private val viewModel by viewModel<CourseOutlineViewModel> {
        parametersOf(courseId, courseTitle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ViewModel automatically injected with all dependencies from DI
    }
}
```

---

## State Management

### Core Pattern: Unidirectional Data Flow (MVVM)

```
User Interaction
        ↓
    Fragment
        ↓ (calls method)
   ViewModel
        ↓ (updates state)
 StateFlow<UIState>
        ↓ (collect)
    Fragment
        ↓ (render)
    UI Update
```

### UIState Pattern (Sealed Classes)

Every screen has a sealed UIState class representing all possible states:

```kotlin
sealed class CourseOutlineUIState {
    object Loading : CourseOutlineUIState()

    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, Int>,
        val courseStatus: CourseComponentStatus,
    ) : CourseOutlineUIState()

    object Error : CourseOutlineUIState()
}
```

**ViewModel Implementation:**
```kotlin
class CourseOutlineViewModel(...) : BaseViewModel() {
    private val _uiState = MutableStateFlow<CourseOutlineUIState>(
        CourseOutlineUIState.Loading
    )
    val uiState: StateFlow<CourseOutlineUIState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                interactor.getCourseStructure(courseId),
                interactor.getCourseStatus(courseId),
                // ... more data sources
            ) { courseStructure, courseStatus, ... ->
                CourseOutlineUIState.CourseData(
                    courseStructure,
                    courseStatus,
                    // ... map all data
                )
            }
            .catch { e ->
                _uiState.value = CourseOutlineUIState.Error
            }
            .collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
```

**Fragment Collection:**
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewLifecycleOwner.lifecycleScope.launch {
        viewModel.uiState.collect { state ->
            when (state) {
                is CourseOutlineUIState.Loading -> showLoadingUI()
                is CourseOutlineUIState.CourseData -> renderCourseData(state)
                is CourseOutlineUIState.Error -> showErrorUI()
            }
        }
    }
}
```

### Message/Event Pattern

For transient events (one-time notifications), use SharedFlow:

```kotlin
class CourseOutlineViewModel(...) : BaseViewModel() {
    // Transient events (not persisted in state)
    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage> = _uiMessage.asSharedFlow()

    private val _navigateToBlock = MutableSharedFlow<String>()
    val navigateToBlock: SharedFlow<String> = _navigateToBlock.asSharedFlow()

    fun navigateToVideoBlock(blockId: String) {
        viewModelScope.launch {
            _navigateToBlock.emit(blockId)
        }
    }

    fun showMessageToUser(message: String) {
        viewModelScope.launch {
            _uiMessage.emit(UIMessage.SnackBar(message))
        }
    }
}
```

**Fragment Handling:**
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    // Messages arrive only once, don't replay
    viewModel.uiMessage.collect { message ->
        when (message) {
            is UIMessage.SnackBar -> showSnackBar(message.text)
            is UIMessage.Toast -> showToast(message.text)
        }
    }
}
```

### Notifier Pattern (Event Bus)

For cross-feature events, use global notifiers:

```kotlin
// Singleton notifiers defined in AppModule
single { CourseNotifier() }
single { VideoNotifier() }
single { DownloadNotifier() }
```

**Notifier Definition:**
```kotlin
class CourseNotifier {
    private val channel = MutableSharedFlow<CourseEvent>(
        replay = 0,
        extraBufferCapacity = 0
    )
    val notifier: Flow<CourseEvent> = channel.asSharedFlow()

    suspend fun send(event: CourseStructureUpdated) = channel.emit(event)
    suspend fun send(event: CourseVideoPositionChanged) = channel.emit(event)
}

sealed class CourseEvent {
    data class CourseStructureUpdated(val courseId: String) : CourseEvent()
    data class CourseVideoPositionChanged(val videoUrl: String, val position: Long) : CourseEvent()
}
```

**Usage in ViewModel:**
```kotlin
init {
    viewModelScope.launch {
        notifier.notifier.collect { event ->
            when (event) {
                is CourseStructureUpdated -> refreshCourseData()
                is CourseVideoPositionChanged -> updateVideoPosition(event)
            }
        }
    }
}
```

### Activity-Scoped Shared State

For state shared between sibling fragments in same Activity:

```kotlin
class SharedViewModel : ViewModel() {
    private val _isFullscreen = MutableLiveData(false)
    val isFullscreen: LiveData<Boolean> = _isFullscreen

    fun setFullscreen(value: Boolean) {
        _isFullscreen.value = value
    }
}

// Fragment usage
class VideoUnitFragment : Fragment() {
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onViewCreated(...) {
        sharedViewModel.isFullscreen.observe(viewLifecycleOwner) { isFullscreen ->
            // React to fullscreen state from sibling fragment
        }
    }
}
```

---

## Module Structure

### Module Hierarchy

```
app (Main application module)
    ├── auth (Authentication)
    ├── core (Foundation, shared code)
    ├── course (Course content)
    ├── dashboard (Dashboard/Home)
    ├── discovery (Course discovery)
    ├── discussion (Discussion forums)
    ├── profile (User profile)
    ├── notifications (Push notifications)
    ├── featuremanagement (Feature flags)
    └── whatsnew (What's New feature)
```

**Dependency Rules:**
- ✅ `course` can depend on `core`
- ✅ `core` can depend on nothing
- ❌ `core` CANNOT depend on `course` (would create cycle)
- ✅ Features can depend on `core` and `auth`
- ❌ Features CANNOT depend on each other (use Notifier instead)

### Module Structure Pattern

Each module follows this structure:

```
module_name/
├── src/main/java/org/openedx/module_name/
│   ├── data/
│   │   ├── api/              # REST APIs
│   │   ├── database/         # Room DAOs & tables
│   │   ├── repository/       # Repository implementations
│   │   └── storage/          # Preferences, shared data
│   │
│   ├── domain/
│   │   ├── interactor/       # Business logic (UseCases)
│   │   └── model/            # Domain models
│   │
│   ├── presentation/
│   │   ├── section1/
│   │   │   ├── Section1Fragment.kt
│   │   │   └── Section1ViewModel.kt
│   │   ├── section2/
│   │   │   ├── Section2Fragment.kt
│   │   │   └── Section2ViewModel.kt
│   │   ├── ModuleAnalytics.kt
│   │   └── ModuleRouter.kt
│   │
│   └── di/
│       └── ModuleModuleProvider.kt
│
├── src/main/res/
│   ├── layout/               # XML layouts
│   ├── drawable/             # Images, vectors
│   ├── values/               # Strings, colors, styles
│   └── ...
│
└── src/test/java/           # Unit tests
```

### Inter-Module Communication

**Option 1: Router Interface (Navigation)**
```kotlin
// Define in origin module
interface CourseRouter {
    fun navigateToCourse(courseId: String)
    fun navigateToCourseSection(courseId: String, sectionId: String)
}

// Implement in app module's AppRouter
class AppRouter : CourseRouter, DiscoveryRouter, ProfileRouter {
    override fun navigateToCourse(courseId: String) {
        // Use fragment manager to navigate
    }
}
```

**Option 2: Notifier (Events)**
```kotlin
// In origin module's ViewModel
viewModelScope.launch {
    courseNotifier.send(CourseDownloadStarted(courseId))
}

// In target module's ViewModel
init {
    viewModelScope.launch {
        notifier.notifier.collect { event ->
            if (event is CourseDownloadStarted) {
                // React to download
            }
        }
    }
}
```

**Option 3: Shared Repository (Shared Data)**
```kotlin
// Both modules depend on core
// Access through DI
single { SharedDataRepository(...) }

// Both modules can access
class ModuleViewModel(
    private val sharedData: SharedDataRepository
) { /* ... */ }
```

---

## Design Patterns

### 1. Repository Pattern (Data Abstraction)

**Purpose:** Single source of truth for data, abstract storage mechanism

```kotlin
interface CourseRepository {
    suspend fun getCourseStructure(courseId: String): CourseStructure
    suspend fun getCourseStructureFlow(courseId: String): Flow<CourseStructure>
    suspend fun updateCourseStatus(courseId: String, status: Status)
}

class CourseRepositoryImpl(
    private val api: CourseApi,
    private val database: Room,
    private val cache: MutableMap<String, CourseStructure>,
) : CourseRepository {
    override suspend fun getCourseStructure(courseId: String): CourseStructure {
        // Check memory → database → API
    }

    override suspend fun getCourseStructureFlow(
        courseId: String
    ): Flow<CourseStructure> = channelFlowWithAwait {
        // Emit: cached → fresh
    }
}
```

### 2. Interactor/UseCase Pattern (Business Logic)

**Purpose:** Orchestrate repositories, apply business logic

```kotlin
class CourseInteractor(
    private val courseRepository: CourseRepository,
    private val downloadRepository: DownloadRepository,
) {
    suspend fun downloadCourse(courseId: String): Flow<DownloadProgress> {
        return courseRepository.getCourseStructure(courseId)
            .flatMapLatest { course ->
                downloadRepository.downloadBlocks(course.blocks)
            }
    }

    suspend fun markCourseComplete(courseId: String) {
        courseRepository.updateCourseStatus(
            courseId,
            Status.COMPLETE
        )
    }
}
```

### 3. ViewModel as Orchestrator (Presentation Logic)

**Purpose:** Combine multiple data sources, manage UI state

```kotlin
class CourseOutlineViewModel(
    private val courseInteractor: CourseInteractor,
    private val analytics: CourseAnalytics,
) : BaseViewModel() {
    init {
        viewModelScope.launch {
            courseInteractor.getCourseStructureFlow(courseId)
                .combine(courseInteractor.getDownloadState(courseId))
                { structure, downloads ->
                    CourseOutlineUIState.CourseData(
                        courseStructure = structure,
                        downloadState = downloads,
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onBlockClicked(blockId: String) {
        analytics.trackBlockClicked(blockId)
        _navigateToBlock.emit(blockId)
    }
}
```

### 4. Adapter Pattern (Legacy/Multi-Type Lists)

```kotlin
class CourseBlockAdapter(
    private val blocks: List<Block>,
    private val clickListener: (Block) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (blocks[position]) {
            is VideoBlock -> VIEW_TYPE_VIDEO
            is HtmlBlock -> VIEW_TYPE_HTML
            is ProblemBlock -> VIEW_TYPE_PROBLEM
            else -> VIEW_TYPE_UNKNOWN
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_VIDEO -> VideoViewHolder(parent)
            VIEW_TYPE_HTML -> HtmlViewHolder(parent)
            else -> UnknownViewHolder(parent)
        }
    }
}
```

### 5. Observer Pattern (Flow Collection)

```kotlin
// Observable (ViewModel)
class MyViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()
}

// Observer (Fragment)
class MyFragment : Fragment() {
    private val viewModel by viewModel<MyViewModel>()

    override fun onViewCreated(...) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // React to state changes
            }
        }
    }
}
```

---

## Tech Stack

### Language & Framework
- **Language:** Kotlin (100%)
- **Min SDK:** 24 (API 24)
- **Target SDK:** 35+ (latest)
- **Build System:** Gradle with Kotlin DSL

### Architecture
- **DI:** Koin 3.x
- **Architecture Pattern:** MVVM + Clean Architecture

### Async/Concurrency
- **Coroutines:** Kotlin Coroutines (1.7+)
- **Flow API:** Kotlin Flow (for streams)
- **Lifecycle Scope:** Jetpack lifecycle-runtime

### UI
- **Modern UI:** Jetpack Compose (gradual adoption)
- **Legacy UI:** XML layouts + AndroidX
- **Navigation:** Jetpack Navigation Component
- **Image Loading:** Coil

### Data
- **Networking:** Retrofit 2
- **JSON Serialization:** Gson
- **Database:** Room (SQLite)
- **Preferences:** SharedPreferences (via PreferencesManager)

### Testing
- **Unit Tests:** JUnit 4
- **Mocking:** Mockk
- **Assertions:** AssertJ

### Analytics & Monitoring
- **Analytics:** Firebase Analytics
- **Crash Reporting:** Firebase Crashlytics
- **Remote Config:** Firebase Remote Config

### Other
- **Media Playback:** ExoPlayer, YouTube Player SDK
- **Payments:** Google Play Billing Library
- **Notifications:** Firebase Cloud Messaging
- **Cast:** Google Cast Framework

---

## Best Practices

### 1. Always Inherit from BaseViewModel

```kotlin
✅ CORRECT
class MyViewModel(...) : BaseViewModel() {
    // Lifecycle-aware, inherits proper cleanup
}

❌ WRONG
class MyViewModel(...) : ViewModel() {
    // Missing base setup
}
```

### 2. Expose Only Readonly State

```kotlin
✅ CORRECT
private val _uiState = MutableStateFlow(UIState.Loading)
val uiState: StateFlow<UIState> = _uiState.asStateFlow()  // Readonly

❌ WRONG
val uiState = MutableStateFlow(UIState.Loading)  // Mutable from outside
```

### 3. Use Sealed Classes for State

```kotlin
✅ CORRECT
sealed class CourseUIState {
    object Loading : CourseUIState()
    data class Success(val course: Course) : CourseUIState()
    data class Error(val message: String) : CourseUIState()
}

❌ WRONG
data class CourseUIState(
    val isLoading: Boolean = false,
    val course: Course? = null,
    val error: String? = null,
)  // Impossible states possible (isLoading && course != null)
```

### 4. Bind Lifecycle Properly

```kotlin
✅ CORRECT
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        // Automatically cancelled onDestroyView
    }
}

❌ WRONG
GlobalScope.launch {
    viewModel.uiState.collect { state ->
        // Leaks even after Fragment destroyed!
    }
}
```

### 5. Inject All Dependencies, Don't Create

```kotlin
✅ CORRECT (via Koin)
class MyViewModel(
    private val repo: Repository,      // Injected
    private val analytics: Analytics,  // Injected
) : BaseViewModel() { }

❌ WRONG
class MyViewModel : BaseViewModel() {
    private val repo = Repository()    // Created locally, not testable
    private val analytics = Analytics()
}
```

### 6. Never Use Global Singletons for State

```kotlin
✅ CORRECT
class MyViewModel : BaseViewModel() {
    private val _state = MutableStateFlow<State>()
    val state: StateFlow<State> = _state.asStateFlow()
    // Cleaned up automatically
}

❌ WRONG
object GlobalState {
    var value: String = ""  // Never cleaned up, memory leak
}
```

### 7. Handle Errors in Repository

```kotlin
✅ CORRECT (Repository handles errors)
suspend fun getCourse(): Flow<Course> = channelFlowWithAwait {
    try {
        send(api.getCourse())
    } catch (e: Exception) {
        emit(database.getCourse())  // Fallback to cached
    }
}

❌ WRONG (ViewModel handles errors)
init {
    viewModelScope.launch {
        try {
            val course = repository.getCourse()
            _uiState.value = Success(course)
        } catch (e: Exception) {
            _uiState.value = Error(e.message)
        }
    }
}
```

### 8. Use Named Qualifiers for Multiple Instances

```kotlin
✅ CORRECT
single(named("MainDispatcher")) { Dispatchers.Main }
single(named("IODispatcher")) { Dispatchers.IO }

viewModel {
    MyViewModel(
        mainDispatcher = get(named("MainDispatcher")),
        ioDispatcher = get(named("IODispatcher")),
    )
}

❌ WRONG
single { Dispatchers.Main }
single { Dispatchers.IO }  // Cannot distinguish when injecting
```

---

## Common Patterns by Feature

### Video Playback Feature

```kotlin
// Presentation
class VideoUnitFragment : Fragment()
class EncodedVideoUnitViewModel : VideoUnitViewModel()

// Domain
class VideoInteractor(
    private val repository: CourseRepository,
    private val notifier: VideoNotifier,
)

// Data
class CourseRepository {
    suspend fun getVideoUrl(blockId: String): String
    suspend fun downloadVideo(blockId: String): Flow<DownloadProgress>
}

// DI
viewModel { (courseId: String, blockId: String) ->
    EncodedVideoUnitViewModel(
        courseId = courseId,
        blockId = blockId,
        context = get(),
        preferencesManager = get(),
        castManager = get(),
        courseRepository = get(),
        notifier = get(),
        notificationManager = get(),
        transcriptManager = get(),
        courseAnalytics = get(),
    )
}
```

### Course Outline Feature

```kotlin
// Presentation
class CourseOutlineFragment : Fragment()
class CourseOutlineViewModel : BaseDownloadViewModel()

// Domain
class CourseInteractor {
    suspend fun getCourseStructure(courseId: String): Flow<CourseStructure>
    suspend fun getCourseStatus(courseId: String): Flow<CourseStatus>
}

// Data
class CourseRepository {
    suspend fun getCourseStructureFlow(courseId: String): Flow<CourseStructure?>
    private val courseStructure = mutableMapOf<String, CourseStructure>()  // Memory cache
}

// DI
viewModel { (courseId: String, courseTitle: String) ->
    CourseOutlineViewModel(
        courseId, courseTitle,
        get(),  // courseInteractor
        get(),  // resourceManager
        get(),  // notifier
        // ... more deps
    )
}
```

### Download Management

```kotlin
// Presentation
class DownloadQueueViewModel : BaseDownloadViewModel()

// Domain
class DownloadInteractor {
    suspend fun downloadCourse(courseId: String): Flow<DownloadProgress>
    suspend fun cancelDownload(courseId: String)
    suspend fun getDownloadState(courseId: String): Flow<DownloadState>
}

// Data
class DownloadRepository {
    suspend fun enqueueDownload(blockId: String): Flow<DownloadProgress>
    suspend fun getDownloadState(blockId: String): Flow<DownloadState>
}

// DI
single { DownloadWorkerController(get(), get(), get()) }
viewModel {
    DownloadQueueViewModel(
        get(),  // downloadInteractor
        get(),  // courseInteractor
        get(),  // downloadNotifier
        // ... more deps
    )
}
```

---

## Anti-Patterns to Avoid

### ❌ Anti-Pattern 1: Global State Singletons

```kotlin
❌ WRONG
object GlobalVideoPlayer {
    var player: ExoPlayer? = null
    var isPlaying: Boolean = false
}

// Usage
GlobalVideoPlayer.player = exoPlayer  // No lifecycle, memory leak

✅ CORRECT
class PipViewModel : ViewModel() {
    private val _playerState = MutableStateFlow<PlayerState>()
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    override fun onCleared() {
        _playerState.value.player?.release()  // Cleanup
    }
}
```

### ❌ Anti-Pattern 2: Logic in Fragments

```kotlin
❌ WRONG
class MyFragment : Fragment() {
    override fun onViewCreated(...) {
        // Business logic here!
        if (condition1 && condition2) {
            // Make API call
            // Update database
            // Calculate result
        }
    }
}

✅ CORRECT
class MyFragment : Fragment() {
    private val viewModel by viewModel<MyViewModel>()

    override fun onViewCreated(...) {
        // Only UI binding
        viewModel.uiState.collect { state ->
            renderUI(state)
        }
    }
}

class MyViewModel : BaseViewModel() {
    // Business logic here
    init {
        if (condition1 && condition2) {
            viewModelScope.launch {
                interactor.doBusinessLogic()
            }
        }
    }
}
```

### ❌ Anti-Pattern 3: Android Dependencies in Domain

```kotlin
❌ WRONG
class CourseInteractor(
    private val context: Context,  // Android dependency!
) {
    fun getCourse(): Course {
        val prefs = context.getSharedPreferences(...)  // Framework code in domain!
        // ...
    }
}

✅ CORRECT
class CourseInteractor(
    private val courseRepository: CourseRepository,  // Abstraction
    private val preferences: CorePreferences,  // Abstraction
) {
    suspend fun getCourse(): Course {
        // Pure Kotlin, no Android imports
        return courseRepository.getCourse()
    }
}
```

### ❌ Anti-Pattern 4: Circular Module Dependencies

```kotlin
❌ WRONG
app module depends on: course, discovery, profile
course module depends on: core
discovery module depends on: course  // ❌ Circular!
                            core

✅ CORRECT
app module depends on: course, discovery, profile
course module depends on: core
discovery module depends on: core  // ✅ Both depend on core, not each other
profile module depends on: core
```

### ❌ Anti-Pattern 5: Direct Notifier Mutations

```kotlin
❌ WRONG
// In Fragment
courseNotifier.send(CourseUpdated())  // Direct mutation of notifier

✅ CORRECT
// In ViewModel
fun onCourseUpdate() {
    viewModelScope.launch {
        courseNotifier.send(CourseUpdated())  // Via ViewModel
    }
}

// In Fragment
viewModel.onCourseUpdate()  // Call ViewModel method
```

### ❌ Anti-Pattern 6: Mutable Exposed State

```kotlin
❌ WRONG
class MyViewModel : BaseViewModel() {
    val uiState = MutableStateFlow(UIState.Loading)  // Mutable from outside!
}

// Anywhere in app
myViewModel.uiState.value = UIState.Error  // Anyone can change state!

✅ CORRECT
class MyViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(UIState.Loading)  // Private
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()  // Readonly

    fun updateState(newState: UIState) {
        _uiState.value = newState  // Only through defined methods
    }
}
```

### ❌ Anti-Pattern 7: Ignoring Lifecycle

```kotlin
❌ WRONG
class MyFragment : Fragment() {
    override fun onCreateView(...): View {
        GlobalScope.launch {  // Ignores Fragment lifecycle!
            api.fetchData()
        }
    }
}

✅ CORRECT
class MyFragment : Fragment() {
    private val viewModel by viewModel<MyViewModel>()

    override fun onViewCreated(...) {
        viewLifecycleOwner.lifecycleScope.launch {  // Lifecycle-aware!
            viewModel.uiState.collect { state ->
                // Automatically cancelled onDestroyView
            }
        }
    }
}
```

---

## File Size & Class Responsibilities

### Recommended Sizes

- **Fragment:** 200-400 lines (UI binding + lifecycle only)
- **ViewModel:** 300-600 lines (state management + orchestration)
- **Interactor:** 200-400 lines (one business unit)
- **Repository:** 300-500 lines per entity
- **API Client:** 100-300 lines (network calls)
- **DAO:** 100-200 lines (database access)

### If a class exceeds recommended size:
1. Split by responsibility
2. Extract helper classes
3. Consider separate features
4. Add tests (tests often indicate over-complexity)

---

## Testing Strategy

### Unit Test Pyramid
```
           /\
          /  \  <-- E2E Tests (few)
         /────\
        /      \  <-- Integration Tests (moderate)
       /────────\
      /          \ <-- Unit Tests (many) ✅
     /────────────\
```

### ViewModel Testing Pattern

```kotlin
@Test
fun testCourseOutlineLoading() {
    // Arrange
    val mockRepository = mockk<CourseRepository>()
    coEvery { mockRepository.getCourseStructure(any()) } returns courseStructure

    val viewModel = CourseOutlineViewModel(
        courseId = "course-1",
        courseTitle = "Title",
        courseInteractor = CourseInteractor(mockRepository),
        // ... mock other deps
    )

    // Act
    advanceUntilIdle()

    // Assert
    assertEquals(
        CourseOutlineUIState.CourseData(courseStructure),
        viewModel.uiState.value
    )
}
```

---

## Monitoring & Analytics

### Application-Scoped Analytics

```kotlin
single { AnalyticsManager(get(), get()) }
single<CourseAnalytics> { get<AnalyticsManager>() }
single<ProfileAnalytics> { get<AnalyticsManager>() }

// Available to all ViewModels
class MyViewModel(
    private val analytics: CourseAnalytics,
) : BaseViewModel() {
    fun onUserAction() {
        analytics.trackAction("block_clicked")
    }
}
```

### Network Monitoring

All network calls through Retrofit with interceptors:
- Request logging
- Error tracking
- Performance monitoring

### Crash Reporting

Firebase Crashlytics automatically captures:
- Uncaught exceptions
- Lifecycle regressions
- Anr events

---

## Resources & Documentation

- **Clean Architecture:** https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- **MVVM Pattern:** https://developer.android.com/jetpack/guide
- **Kotlin Coroutines:** https://kotlinlang.org/docs/coroutines-overview.html
- **Koin Documentation:** https://insert-koin.io
- **Jetpack Compose:** https://developer.android.com/jetpack/compose

---

**Document Version:** 1.0
**Last Updated:** April 2026
**Maintainer:** EDX Engineering Team
