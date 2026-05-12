# PiP Implementation - Audit & Fixes Summary

**Date:** 13 May 2026  
**Branch:** `sandeepd/Learner-10967`  
**Repository:** `edx/edx-mobile-marketplace-android`  
**Status:** ✅ Fully aligned with Architecture Summary

---

## Audit Result: Current Repo vs Architecture Summary

### ✅ 7 New Files — All Present

| # | File | Path | Status |
|---|------|------|--------|
| 1 | **PlayerController.kt** | `course/.../data/repository/player/PlayerController.kt` | ✅ Created |
| 2 | **PipPlayerState.kt** | `course/.../domain/model/PipPlayerState.kt` | ✅ Created |
| 3 | **PipPlayerRepository.kt** | `course/.../data/repository/PipPlayerRepository.kt` | ✅ Created |
| 4 | **PipBroadcastReceiverManager.kt** | `course/.../data/repository/PipBroadcastReceiverManager.kt` | ✅ Created |
| 5 | **PipInteractor.kt** | `course/.../domain/interactor/PipInteractor.kt` | ✅ Created |
| 6 | **PipViewModel.kt** | `course/.../presentation/unit/video/PipViewModel.kt` | ✅ Created |
| 7 | **PipTest.kt** | `course/.../test/.../PipTest.kt` | ✅ Created |

### ✅ 3 Existing Files Modified

| File | Integration Points | Status |
|------|-------------------|--------|
| **ScreenModule.kt** | `single { PipPlayerRepository() }`, `factory { PipBroadcastReceiverManager }`, `factory { PipInteractor }`, `viewModel { PipViewModel }` | ✅ 4 DI entries |
| **VideoUnitFragment.kt** | `pipViewModel`, `pipReceiverManager`, `pipState` observer, `registerPlayer`, `enterPipMode`/`exitPipMode`, `onStart`/`onStop` lifecycle | ✅ All hooks |
| **YoutubeVideoUnitFragment.kt** | Same as above + `ytController`, `onStart`/`onStop`, `onDestroyView` cleanup | ✅ All hooks |

### ✅ 2 Old Files Deleted

| File | Description | Status |
|------|-------------|--------|
| `PipPlayerController.kt` | Global singleton object (architecture violation) | ✅ Deleted |
| `YoutubePipActionReceiver` | Old broadcast receiver + manifest `<receiver>` entry | ✅ Deleted |

### ✅ Key Design Decisions Verified

| Decision | Implementation | Status |
|----------|---------------|--------|
| Activity-scoped ViewModel | `by viewModel(ownerProducer = { requireActivity() })` | ✅ |
| Koin DI everywhere | No direct instantiation, all `by inject()` / `by viewModel()` | ✅ |
| Unified BroadcastReceiver | `pipReceiverManager.register()` in `onStart()`, `.unregister()` in `onStop()` — both fragments | ✅ |
| Player abstraction | `ExoPlayerController` + `YouTubePlayerController` implementing `PlayerController` interface | ✅ |
| StateFlow-based state | `pipViewModel.pipState` observed in both fragments | ✅ |
| No global singletons | Zero violations found | ✅ |

---

## Bugs Found & Fixed During Implementation

### Bug 1: `PlayerType` Enum Name Collision

**Symptom:** Build error — unresolved reference `PlayerType.EXOPLAYER`  
**Root Cause:** New `PlayerType` enum (NONE/EXOPLAYER/YOUTUBE) in `PipPlayerState.kt` collided with existing `PlayerType` enum (EXO_REGULAR/CHROME_CAST/NONE) in `PlayerState.kt` — both in the same package `org.openedx.course.domain.model`.  
**Fix:** Renamed to `PipPlayerType` across all 6 source files.  
**Files Changed:**
- `PipPlayerState.kt` — enum renamed
- `PipPlayerRepository.kt` — import + usages
- `PipInteractor.kt` — import + usages
- `PipViewModel.kt` — import + usages
- `VideoUnitFragment.kt` — import + usages
- `YoutubeVideoUnitFragment.kt` — import + usages
- `PipTest.kt` — import + usages

---

### Bug 2: `Cannot create instance of PipViewModel`

**Symptom:** `RuntimeException: Cannot create an instance of class PipViewModel`  
**Root Cause:** Both fragments initially used `by activityViewModels<PipViewModel>()` which uses Android's default `ViewModelProvider.Factory`. This factory requires a no-arg constructor, but `PipViewModel` takes `PipInteractor` as a constructor parameter — only Koin can resolve this dependency.  
**Fix:** Changed to `by viewModel(ownerProducer = { requireActivity() })` which uses Koin's factory to resolve dependencies while still scoping the ViewModel to the activity.  
**Files Changed:**
- `VideoUnitFragment.kt` — ViewModel delegation changed
- `YoutubeVideoUnitFragment.kt` — ViewModel delegation changed

---

### Bug 3: PiP Play/Pause Buttons Not Working (YouTube)

**Symptom:** Tapping play/pause buttons in PiP window had no effect for YouTube videos. ExoPlayer PiP buttons also didn't refresh properly after tapping.  
**Root Cause:** Two issues:
1. **`YoutubeVideoUnitFragment`** was missing `onStart()` / `onStop()` lifecycle methods, so `pipReceiverManager.register()` was never called — the BroadcastReceiver was never registered, meaning PiP button intents were never received.
2. **Both fragments** lacked a `pipState` flow observer, so after the BroadcastReceiver handled a play/pause action and updated the repository state, the PiP action buttons were not refreshed to reflect the new state (e.g., switching from play → pause icon).

**Fix:**
- Added `onStart()` with `pipReceiverManager.register()` and `onStop()` with `pipReceiverManager.unregister()` to `YoutubeVideoUnitFragment`
- Added `pipReceiverManager.unregister()` in `onDestroyView()` as safety cleanup
- Added `pipViewModel.pipState` flow observer in `onViewCreated()` of both fragments that calls `updatePipActions()` whenever state changes while in PiP mode
- Added required imports (`lifecycleScope`, `launchIn`, `onEach`, `ConstraintSet`)

**Files Changed:**
- `VideoUnitFragment.kt` — added `pipState` observer in `onViewCreated()`
- `YoutubeVideoUnitFragment.kt` — added `onStart()`, `onStop()`, `onDestroyView()` cleanup, `pipState` observer, and imports

---

### Bug 4: Video Replay After End in PiP Mode

**Symptom:** After video ends in PiP, the play/replay button showed but tapping it didn't restart the video from the beginning.  
**Root Cause:** This was a side-effect of Bug 3. The replay flow works as:
1. `ACTION_PLAY` broadcast → `repository.play()` → `controller.play()` → detects ended state → `restart()` (seekTo(0) + play)
2. State update triggers observer → `updatePipActions()` refreshes to show pause button

With the BroadcastReceiver not registered (Bug 3), the entire chain never started.  
**Fix:** Same as Bug 3 — registering the receiver and adding the state observer resolved this.

---

## Architecture Layer Summary

```
PRESENTATION LAYER
├── VideoUnitFragment.kt (MODIFIED)
│   ├── pipViewModel: PipViewModel (activity-scoped via Koin)
│   ├── pipReceiverManager: PipBroadcastReceiverManager (injected via Koin)
│   ├── pipState observer (onViewCreated)
│   ├── onStart() → register receiver
│   ├── onStop() → unregister receiver (if not in PiP)
│   ├── onDestroy() → unregister player
│   ├── onPictureInPictureModeChanged() → enter/exit PiP
│   └── updatePipActions() / showReplayAction()
│
├── YoutubeVideoUnitFragment.kt (MODIFIED)
│   ├── pipViewModel: PipViewModel (activity-scoped via Koin)
│   ├── pipReceiverManager: PipBroadcastReceiverManager (injected via Koin)
│   ├── ytController: YouTubePlayerController
│   ├── pipState observer (onViewCreated)
│   ├── onStart() → register receiver
│   ├── onStop() → unregister receiver (if not in PiP)
│   ├── onDestroyView() → unregister receiver + player, null controller
│   ├── onPictureInPictureModeChanged() → enter/exit PiP
│   └── updatePipActions()
│
└── PipViewModel.kt (NEW)
    ├── pipState: StateFlow<PipPlayerState>
    ├── registerPlayer() / unregisterPlayer()
    ├── enterPipMode() / exitPipMode()
    ├── updatePlaybackState()
    └── onCleared() → cleanup

DOMAIN LAYER
├── PipInteractor.kt (NEW)
│   ├── Delegates to PipPlayerRepository
│   ├── Exposes pipState: StateFlow
│   └── handleAction() for PipAction sealed class
│
└── PipPlayerState.kt (NEW)
    ├── PipPlayerState data class (playerType, isPlaying, isEnded, isPipMode)
    ├── PipPlayerType enum (NONE, EXOPLAYER, YOUTUBE)
    └── PipAction sealed class

DATA LAYER
├── PipPlayerRepository.kt (NEW)
│   ├── Single source of truth: MutableStateFlow<PipPlayerState>
│   ├── registerPlayer() / unregisterPlayer()
│   ├── updatePlaybackState()
│   ├── enterPipMode() / exitPipMode()
│   └── play() / pause() / seekForward() / seekBackward() / restart()
│
├── PipBroadcastReceiverManager.kt (NEW)
│   ├── register() / unregister() tied to fragment lifecycle
│   ├── Inner BroadcastReceiver delegates to PipPlayerRepository
│   └── Companion: ACTION_PLAY/PAUSE/FORWARD/REWIND, REQUEST_PLAY/PAUSE/FORWARD/REWIND
│
└── PlayerController.kt (NEW)
    ├── PlayerController interface (play, pause, seek, restart, isPlaying, isEnded, etc.)
    ├── ExoPlayerController (wraps Media3 Player)
    └── YouTubePlayerController (wraps YouTubePlayer with manual state tracking)

DI REGISTRATION (ScreenModule.kt)
├── single { PipPlayerRepository() }
├── factory { PipBroadcastReceiverManager(get(), get()) }
├── factory { PipInteractor(get()) }
└── viewModel { PipViewModel(get()) }
```

---

## Koin DI Wiring

```
PipViewModel
    └── PipInteractor
            └── PipPlayerRepository  (singleton)

PipBroadcastReceiverManager
    ├── Context  (Android application context)
    └── PipPlayerRepository  (singleton, same instance)
```

- `PipPlayerRepository` is a **singleton** — one instance shared app-wide
- `PipBroadcastReceiverManager` is a **factory** — each fragment gets its own instance (correct, since each manages its own receiver lifecycle)
- `PipInteractor` is a **factory** — stateless orchestrator
- `PipViewModel` is a **viewModel** — activity-scoped via `ownerProducer`

---

## Files Checklist

### New Files (7)

- [x] `course/src/main/java/org/openedx/course/data/repository/player/PlayerController.kt`
- [x] `course/src/main/java/org/openedx/course/domain/model/PipPlayerState.kt`
- [x] `course/src/main/java/org/openedx/course/data/repository/PipPlayerRepository.kt`
- [x] `course/src/main/java/org/openedx/course/data/repository/PipBroadcastReceiverManager.kt`
- [x] `course/src/main/java/org/openedx/course/domain/interactor/PipInteractor.kt`
- [x] `course/src/main/java/org/openedx/course/presentation/unit/video/PipViewModel.kt`
- [x] `course/src/test/java/org/openedx/course/presentation/unit/video/PipTest.kt`

### Modified Files (3)

- [x] `app/src/main/java/org/openedx/app/di/ScreenModule.kt` — 4 DI entries added
- [x] `course/src/main/java/org/openedx/course/presentation/unit/video/VideoUnitFragment.kt`
- [x] `course/src/main/java/org/openedx/course/presentation/unit/video/YoutubeVideoUnitFragment.kt`

### Deleted Files (2)

- [x] `PipPlayerController.kt` — global singleton removed
- [x] `YoutubePipActionReceiver` — old receiver class + manifest entry removed

---

## Violations Fixed

| # | Violation | Before | After |
|---|-----------|--------|-------|
| 1 | Global Singleton for State | `PipPlayerController` object | `PipPlayerRepository` via Koin singleton |
| 2 | No Dependency Injection | Direct `PipPlayerController` access | All deps through Koin DI |
| 3 | No ViewModel-based State | State split between fragment + singleton | `PipViewModel` + `StateFlow` |
| 4 | Inconsistent Patterns | ExoPlayer and YouTube different approaches | Identical pattern via `PlayerController` interface |
| 5 | Untestable Code | Singletons cannot be mocked | 100% testable via DI (3 test classes, ~30 tests) |

---

**Document Version:** 1.0  
**Last Updated:** 13 May 2026  
**Author:** Automated Audit
