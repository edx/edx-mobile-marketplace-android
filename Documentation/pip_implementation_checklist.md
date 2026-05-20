# PiP Refactoring - Complete File List & Action Items

**Status:** Ready for Implementation
**Timeline:** 2-3 weeks
**Complexity:** Low (reuse existing fragments)
**Files to Create:** 7 new files
**Files to Modify:** 3 existing files
**Files to Delete:** 2 old files

---

## NEW FILES TO CREATE

### Data Layer (3 files)

#### 1. `course/src/main/java/org/openedx/course/data/repository/player/PlayerController.kt`
**Purpose:** Player abstraction interface
**Size:** ~80 lines
**Contains:**
- `PlayerController` interface
- `ExoPlayerController` implementation
- `YouTubePlayerController` implementation

#### 2. `course/src/main/java/org/openedx/course/data/repository/PipPlayerRepository.kt`
**Purpose:** PiP player state management repository
**Size:** ~200 lines
**Contains:**
- `PipPlayerRepository` class
- 3-level caching logic
- Player lifecycle management
- PiP mode transitions

#### 3. `course/src/main/java/org/openedx/course/data/repository/PipBroadcastReceiverManager.kt`
**Purpose:** BroadcastReceiver lifecycle management
**Size:** ~100 lines
**Contains:**
- `PipBroadcastReceiverManager` class
- `PipActionReceiver` inner class
- Register/unregister logic

### Domain Layer (2 files)

#### 4. `course/src/main/java/org/openedx/course/domain/model/PipPlayerState.kt`
**Purpose:** Domain models for PiP
**Size:** ~50 lines
**Contains:**
- `PipPlayerState` data class
- `PlayerType` enum
- `AspectRatio` enum
- `PipEvent` sealed class

#### 5. `course/src/main/java/org/openedx/course/domain/interactor/PipInteractor.kt`
**Purpose:** PiP business logic
**Size:** ~150 lines
**Contains:**
- `PipInteractor` class
- Player state flow
- PiP event flow
- Business logic orchestration

### Presentation Layer (1 file)

#### 6. `course/src/main/java/org/openedx/course/presentation/unit/video/PipViewModel.kt`
**Purpose:** PiP state management ViewModel
**Size:** ~180 lines
**Contains:**
- `PipViewModel` class
- `PipActionEvent` sealed class
- State and event observers
- Action handlers

### Testing (1 file - complete suite)

#### 7. `course/src/test/java/org/openedx/course/presentation/unit/video/PipTest.kt`
**Purpose:** All tests for PiP system
**Size:** ~450 lines (3 test classes)
**Contains:**
- `PipPlayerRepositoryTest`
- `PipInteractorTest`
- `PipViewModelTest`

---

## EXISTING FILES TO MODIFY

### 1. `course/src/main/java/org/openedx/course/presentation/unit/video/VideoUnitFragment.kt`
**Changes:** Add PiP support
**Details:**
- Add 10 import statements
- Add 2 properties (pipViewModel, pipReceiverManager)
- Modify 4 lifecycle methods (onViewCreated, onStart, onStop, onDestroy)
- Add 7 new methods for PiP
- ~150-200 lines added

**Key additions:**
```
setupPipObservers()
initializePiPSystem(player)
onPipButtonClicked()
updatePipUI()
handlePipAction()
```

### 2. `course/src/main/java/org/openedx/course/presentation/unit/video/YoutubeVideoUnitFragment.kt`
**Changes:** Add PiP support (identical to ExoPlayer)
**Details:**
- Identical to VideoUnitFragment changes
- Use YouTubePlayer-specific code in initializePiPSystem()
- ~150-200 lines added

### 3. `app/src/main/java/org/openedx/app/di/ScreenModule.kt`
**Changes:** Register PiP dependencies
**Details:**
- Add 5 DI registration entries
- Lines to add: ~15-20 lines

**Entries to add:**
```kotlin
single { PipPlayerRepository(get()) }
single { (pipRepository: PipPlayerRepository) ->
    PipBroadcastReceiverManager(get(), pipRepository)
}
factory { PipInteractor(get()) }
viewModel { PipViewModel(pipInteractor = get()) }
```

---

## EXISTING FILES TO DELETE

### 1. Delete: `course/src/main/java/org/openedx/course/PipPlayerController.kt`
**Reason:** Replaced by PipPlayerRepository + PipViewModel
**Status:** Global singleton (VIOLATES architecture)

### 2. Delete: `course/src/main/java/org/openedx/course/YoutubePipActionReceiver.kt`
**Reason:** Replaced by PipBroadcastReceiverManager
**Status:** Independent receiver management (now unified)

---

## IMPLEMENTATION SEQUENCE

### Phase 1: Data & Domain Layers (Week 1)

1. **Create files:**
   - [ ] PlayerController.kt
   - [ ] PipPlayerState.kt (domain model)
   - [ ] PipPlayerRepository.kt
   - [ ] PipBroadcastReceiverManager.kt
   - [ ] PipInteractor.kt

2. **Verify:**
   - [ ] All imports compile
   - [ ] No circular dependencies
   - [ ] Clean architecture maintained

3. **Test:**
   - [ ] Create PipPlayerRepositoryTest
   - [ ] Create PipInteractorTest
   - [ ] Run tests (should pass)

### Phase 2: Presentation Layer (Week 2)

1. **Create files:**
   - [ ] PipViewModel.kt

2. **Register in DI:**
   - [ ] Add 5 entries to ScreenModule.kt
   - [ ] Verify compilation

3. **Modify VideoUnitFragment:**
   - [ ] Add imports
   - [ ] Add properties
   - [ ] Modify lifecycle methods
   - [ ] Add PiP methods
   - [ ] Wire up PiP button

4. **Modify YoutubeVideoUnitFragment:**
   - [ ] Repeat steps from VideoUnitFragment
   - [ ] Use YouTube-specific code

5. **Test:**
   - [ ] Create PipViewModelTest
   - [ ] Create integration test
   - [ ] Run full test suite

### Phase 3: Testing & Validation (Week 2-3)

1. **Unit Tests:**
   - [ ] All PipPlayerRepositoryTest pass
   - [ ] All PipInteractorTest pass
   - [ ] All PipViewModelTest pass

2. **Integration Testing:**
   - [ ] Manual PiP entry/exit
   - [ ] Player controls work
   - [ ] State persistence
   - [ ] Lifecycle safety
   - [ ] Configuration changes

3. **Regression Testing:**
   - [ ] Existing video playback unaffected
   - [ ] Existing PiP was removed before tests
   - [ ] No crashes or leaks

### Phase 4: Cleanup & Documentation (Week 3-4)

1. **Code Cleanup:**
   - [ ] Delete old PipPlayerController.kt
   - [ ] Delete old YoutubePipActionReceiver.kt
   - [ ] Search for remaining references

2. **Documentation:**
   - [ ] Update ARCHITECTURE.md (PiP section)
   - [ ] Update project README
   - [ ] Add code comments

3. **Final Review:**
   - [ ] Code review checklist
   - [ ] Performance review
   - [ ] Security review
   - [ ] Merge to main

---

## Dependency Map

```
Architecture dependency flow:

VideoUnitFragment (MODIFIED)
    ↓ uses
PipViewModel (NEW)
    ↓ uses
PipInteractor (NEW)
    ↓ uses
PipPlayerRepository (NEW)
    ↓ uses
PlayerController (NEW)
    ↓ uses
ExoPlayer (existing)

YoutubeVideoUnitFragment (MODIFIED)
    ↓ uses (same as above)
    ↓ but with
YouTubePlayer (existing)

Broadcast Receiver flow:
PlayerController
    ↓ listens to
PipBroadcastReceiverManager (NEW)
    ↓ communicates to
PipViewModel (NEW)
    ↓ updates
VideoUnitFragment UI (MODIFIED)
```

---

## Quick Stats

| Metric | Count |
|--------|-------|
| **Total lines of code added** | ~1,200 |
| **Total lines of code modified** | ~400 |
| **Total lines of code deleted** | ~150 |
| **New files created** | 7 |
| **Existing files modified** | 3 |
| **Existing files deleted** | 2 |
| **Test files created** | 1 (with 3 test classes) |
| **Estimated effort** | 2-3 weeks |
| **Complexity** | Low |
| **Risk** | Very Low |

---

## Before & After Code Comparison

### BEFORE (WRONG ❌)
```kotlin
// Violates all architecture patterns
object PipPlayerController {
    var player: YouTubePlayer? = null  // Global singleton
}

// Usage in Fragment
PipPlayerController.player = youtubePlayer
if (PipPlayerController.isPlaying) {
    PipPlayerController.pause()
}
```

### AFTER (CORRECT ✅)
```kotlin
// Follows all architecture patterns
class PipViewModel(
    private val pipInteractor: PipInteractor
) : BaseViewModel() {
    private val _pipState = MutableStateFlow<PipPlayerState>()
    val pipState: StateFlow<PipPlayerState> = _pipState.asStateFlow()
}

// Usage in Fragment
private val pipViewModel by activityViewModels<PipViewModel>()

fun onPipButtonClicked() {
    viewLifecycleOwner.lifecycleScope.launch {
        pipViewModel.requestEnterPipMode(requireContext())
    }
}
```

---

## Validation Checklist

### Architecture & DI
- [ ] All dependencies injected via Koin
- [ ] No global singletons for state (except Notifiers)
- [ ] ViewModel owns all state
- [ ] Repository is single source of truth
- [ ] Interactor has no Android dependencies
- [ ] No circular dependencies

### Lifecycle & Safety
- [ ] State automatically cleaned up in onCleared()
- [ ] BroadcastReceiver registered in onStart()
- [ ] BroadcastReceiver unregistered in onStop()
- [ ] Player released in onDestroy()
- [ ] No memory leaks

### Patterns & Consistency
- [ ] Both players use identical DI pattern
- [ ] Sealed classes for UIState
- [ ] StateFlow for main state
- [ ] SharedFlow for events
- [ ] One ViewModel per screen concept
- [ ] Activity-scoped when needed

### Testing
- [ ] All Data layer tests pass
- [ ] All Domain layer tests pass
- [ ] All Presentation layer tests pass
- [ ] Coverage > 80% for PiP code
- [ ] No flaky tests

### Code Quality
- [ ] No warnings or errors
- [ ] Code formatted correctly
- [ ] No unused imports
- [ ] No commented-out code
- [ ] Clear method names and documentation
- [ ] Proper error handling

---

## Common Errors & How to Avoid

| Error | Cause | Prevention |
|-------|-------|-----------|
| "Cannot find PipViewModel" | Not registered in DI | Check ScreenModule.kt entry |
| "pipViewModel is null" | Fragment not attached to activity | Use `by activityViewModels()` |
| "BroadcastReceiver not working" | Not registered in onStart | Check onStart() implementation |
| "StateFlow not updating UI" | Not collected in viewLifecycleOwner.lifecycleScope | Check setupPipObservers() |
| "Memory leak detected" | Player not released | Check onDestroy() cleanup |
| "Old PipPlayerController still used" | Didn't delete old file | Verify deletion and search for refs |

---

## Success Criteria

✅ **Compilation:** Code compiles without warnings
✅ **Tests:** All tests pass (unit + integration)
✅ **Architecture:** Follows all patterns from ARCHITECTURE.md
✅ **Functionality:** PiP works identically for ExoPlayer and YouTube
✅ **Performance:** No memory leaks, startup time unchanged
✅ **Consistency:** Both players use same code patterns
✅ **Documentation:** Clear comments and docstrings
✅ **Code Review:** Approved by team lead

---

**Document Version:** 1.0
**Created:** April 2026
**Complexity:** Low
**Estimated Timeline:** 2-3 weeks
**Risk Level:** Very Low
