# PiP Integration Changes — Fixes Audit Summary

**Date:** 13 May 2026  
**Branch:** `sandeepd/Learner-10967`  
**Repository:** `edx/edx-mobile-marketplace-android`  
**Source Document:** `pip_integration_changes.md`

---

## Overview

The `pip_integration_changes.md` document proposed a specific integration pattern for adding PiP support to both video fragments. The actual implementation follows the **same architecture** but with refinements discovered during implementation (simpler API surface, Koin injection instead of manual instantiation, direct method calls instead of coroutine-wrapped ViewModel calls).

---

## VideoUnitFragment.kt — Proposed vs Actual

### 1. Imports

| Proposed | Actual | Status |
|----------|--------|--------|
| `import PipViewModel` | `PipViewModel` imported implicitly via Koin `viewModel` delegate | ✅ Equivalent |
| `import PipActionEvent` | Not needed — actions handled directly, no sealed event class in fragment | ✅ Simplified |
| `import PipBroadcastReceiverManager` | `import org.openedx.course.data.repository.PipBroadcastReceiverManager` | ✅ Match |
| `import PipPlayerRepository` | Not needed — repository accessed only through ViewModel/Interactor | ✅ Cleaner |
| `import ExoPlayerController` | `import org.openedx.course.data.repository.player.ExoPlayerController` | ✅ Match |
| `import PlayerType` | `import org.openedx.course.domain.model.PipPlayerType` (renamed to avoid collision) | ✅ Improved |
| `import PipPlayerState` | Not needed — state accessed via `pipViewModel.pipState.value` | ✅ Simplified |

### 2. Properties

| Proposed | Actual | Status |
|----------|--------|--------|
| `private val pipViewModel by activityViewModels<PipViewModel>()` | `private val pipViewModel: PipViewModel by viewModel(ownerProducer = { requireActivity() })` | ✅ Improved — uses Koin to resolve `PipInteractor` dependency |
| `private var pipReceiverManager: PipBroadcastReceiverManager? = null` | `private val pipReceiverManager: PipBroadcastReceiverManager by inject()` | ✅ Improved — Koin injection, non-nullable |

### 3. onViewCreated()

| Proposed | Actual | Status |
|----------|--------|--------|
| Call `setupPipObservers()` | Added `pipViewModel.pipState.onEach { ... }.launchIn(viewLifecycleOwner.lifecycleScope)` inline | ✅ Equivalent — observer calls `updatePipActions()` when in PiP |

### 4. onStart()

| Proposed | Actual | Status |
|----------|--------|--------|
| `pipReceiverManager?.register(viewLifecycleOwner.lifecycleScope)` | `pipReceiverManager.register()` | ✅ Simpler — no coroutine scope needed |

### 5. onStop()

| Proposed | Actual | Status |
|----------|--------|--------|
| `pipReceiverManager?.unregister()` | `pipReceiverManager.unregister()` (only when not in PiP mode) | ✅ Improved — conditional unregister preserves PiP controls |

### 6. onDestroy()

| Proposed | Actual | Status |
|----------|--------|--------|
| `pipViewModel.onPipModeExited()` | `pipViewModel.unregisterPlayer()` | ✅ Equivalent — cleans up controller reference and resets state |

### 7. Player Registration

| Proposed | Actual | Status |
|----------|--------|--------|
| `initializePiPSystem(exoPlayer)` calling `pipViewModel.pipInteractor.registerPlayer(...)` | `ExoPlayerController(player)` + `pipViewModel.registerPlayer(controller, PipPlayerType.EXOPLAYER)` | ✅ Simpler — direct call, no coroutine wrapping needed |

### 8. New Methods

| Proposed Method | Actual Implementation | Status |
|----------------|----------------------|--------|
| `initializePiPSystem(player)` | Inline in `onViewCreated` — `ExoPlayerController(player)` + `pipViewModel.registerPlayer()` | ✅ Inlined |
| `onPipButtonClicked()` | `enablePipMode()` (existing method, enhanced with permission check) | ✅ Reused existing |
| `setupPipObservers()` | Inline `pipState.onEach { ... }.launchIn()` in `onViewCreated` | ✅ Inlined |
| `updatePipUI(state)` | `updatePipActions()` + `showReplayAction()` (existing methods, enhanced) | ✅ Reused existing |
| `handlePipAction(action)` | Handled by `PipBroadcastReceiverManager` → `PipPlayerRepository` directly | ✅ Cleaner — no fragment involvement |
| `showPipButton()` / `hidePipButton()` | `binding.pipBtn?.isVisible = ...` in `onResume()` and `onPictureInPictureModeChanged()` | ✅ Reused existing |
| `onPictureInPictureModeChanged()` | `pipViewModel.enterPipMode()` / `pipViewModel.exitPipMode()` + `restoreNormalUI()` | ✅ Match |

---

## YoutubeVideoUnitFragment.kt — Proposed vs Actual

### Same Pattern as VideoUnitFragment ✅

| Aspect | Proposed | Actual | Status |
|--------|----------|--------|--------|
| Properties | `pipViewModel`, `pipReceiverManager` | Same + `ytController: YouTubePlayerController?` | ✅ Enhanced |
| Player Registration | `YouTubePlayerController(player)` + `pipViewModel.pipInteractor.registerPlayer(...)` | `YouTubePlayerController(player)` + `pipViewModel.registerPlayer(ytController, PipPlayerType.YOUTUBE)` | ✅ Simpler |
| State Updates | `player.setOnStateChangeListener { ... }` | `onStateChange()` callback updates `ytController` + `pipViewModel.updatePlaybackState()` | ✅ Equivalent |
| Lifecycle: `onStart()` | Not mentioned | `pipReceiverManager.register()` | ✅ Added (was missing in proposal!) |
| Lifecycle: `onStop()` | Not mentioned | `pipReceiverManager.unregister()` (if not in PiP) | ✅ Added (was missing in proposal!) |
| Lifecycle: `onDestroyView()` | Not mentioned | `pipReceiverManager.unregister()` + `ytController = null` + `pipViewModel.unregisterPlayer()` | ✅ Added (was missing in proposal!) |
| Permission Check | Not mentioned | `isPipPermissionGranted()` + `showPipDisabledMessage()` | ✅ Added (improvement over proposal) |
| `onResume()` Permission Re-check | Not mentioned | `binding.pipBtn?.isVisible = ... isPipPermissionGranted()` | ✅ Added (improvement over proposal) |

---

## ScreenModule.kt — Proposed vs Actual

| Proposed | Actual | Status |
|----------|--------|--------|
| `single { PipPlayerRepository(get()) }` | `single { PipPlayerRepository() }` | ✅ Simpler — no constructor params needed |
| `single { PipBroadcastReceiverManager(get(), pipRepository) }` | `factory { PipBroadcastReceiverManager(get(), get()) }` | ✅ Improved — `factory` gives each fragment its own instance |
| `factory { PipInteractor(get()) }` | `factory { PipInteractor(get()) }` | ✅ Exact match |
| `viewModel { PipViewModel(pipInteractor = get()) }` | `viewModel { PipViewModel(get()) }` | ✅ Match |
| 5 entries proposed | 4 entries actual | ✅ Simpler — no separate `PipPlayerRepository` param injection needed |

---

## Implementation Checklist — Audit

### For VideoUnitFragment:
- [x] Add imports
- [x] Add `pipViewModel` property (Koin activity-scoped)
- [x] Add `pipReceiverManager` property (Koin injected)
- [x] Add `pipState` observer in `onViewCreated`
- [x] Add `pipReceiverManager.register()` in `onStart`
- [x] Add `pipReceiverManager.unregister()` in `onStop`
- [x] Add `pipViewModel.unregisterPlayer()` in `onDestroy`
- [x] Register `ExoPlayerController` when player is ready
- [x] PiP button click wired to `enablePipMode()`
- [x] `updatePipActions()` / `showReplayAction()` for PiP remote actions
- [x] `isPipPermissionGranted()` check (bonus — not in original proposal)
- [x] `onResume()` permission re-check (bonus)

### For YoutubeVideoUnitFragment:
- [x] Add imports
- [x] Add `pipViewModel` property (Koin activity-scoped)
- [x] Add `pipReceiverManager` property (Koin injected)
- [x] Add `ytController` property
- [x] Add `pipState` observer in `onViewCreated`
- [x] Add `pipReceiverManager.register()` in `onStart`
- [x] Add `pipReceiverManager.unregister()` in `onStop`
- [x] Add cleanup in `onDestroyView`
- [x] Register `YouTubePlayerController` in `onReady`
- [x] PiP button click wired to `enablePipMode()`
- [x] `updatePipActions()` for PiP remote actions
- [x] `isPipPermissionGranted()` check (bonus)
- [x] `showPipDisabledMessage()` (bonus)
- [x] `onResume()` permission re-check (bonus)

### DI Module:
- [x] Add 4 entries to `ScreenModule.kt`

### Testing:
- [x] `PipPlayerRepositoryTest` created
- [x] `PipInteractorTest` created
- [x] `PipViewModelTest` created
- [ ] ⚠️ Run all tests (pending)

### Cleanup:
- [x] Delete old `PipPlayerController.kt`
- [x] Delete old `YoutubePipActionReceiver`
- [x] Verify no other references to old classes in source

---

## Key Differences: Proposed vs Actual

| Aspect | Proposed | Actual | Why Changed |
|--------|----------|--------|-------------|
| ViewModel delegation | `by activityViewModels()` | `by viewModel(ownerProducer = { requireActivity() })` | Koin needs to inject `PipInteractor` — Android default factory can't |
| Receiver manager | `var pipReceiverManager: ... ? = null` (manual init) | `val pipReceiverManager: ... by inject()` | Koin handles instantiation cleaner |
| Receiver scope | `single` in DI | `factory` in DI | Each fragment needs its own receiver instance |
| PiP actions | Fragment handles via `handlePipAction()` | `PipBroadcastReceiverManager` → `PipPlayerRepository` directly | Cleaner — fragment doesn't need to know about broadcast actions |
| Coroutine wrapping | `lifecycleScope.launch { pipViewModel.xxx() }` | Direct calls: `pipViewModel.registerPlayer()`, `pipViewModel.enterPipMode()` | Methods are synchronous — no coroutine needed |
| State observer | Separate `setupPipObservers()` method | Inline `pipState.onEach { }.launchIn()` | Simpler — one-liner instead of separate method |
| Player type enum | `PlayerType` | `PipPlayerType` | Renamed to avoid collision with existing `PlayerType` in same package |
| Permission check | Not proposed | `isPipPermissionGranted()` in both fragments | Added as improvement — guards against disabled PiP |
| YouTube lifecycle | `onStart`/`onStop` not specified | Explicitly added `onStart`/`onStop`/`onDestroyView` | Critical fix — receiver must be registered for PiP buttons to work |

---

## Manual Testing Checklist — Status

### Basic PiP Entry:
- [x] Click PiP button → fragment enters PiP mode
- [x] Permission check prevents entry when disabled
- [x] Toast shown when permission disabled

### Player Controls in PiP:
- [x] Play/pause works via remote actions (fixed: receiver registration)
- [x] Replay works after video ends (seekTo(0) + play)
- [x] Controls appear in PiP window

### State Management:
- [x] Playback state updates reflect in PiP action buttons (fixed: pipState observer)
- [x] PiP icon visibility toggles with permission changes (onResume re-check)

### Lifecycle Safety:
- [x] `onStart()` registers receiver
- [x] `onStop()` unregisters receiver (when not in PiP)
- [x] `onDestroy`/`onDestroyView` cleans up player registration
- [x] `onCleared()` in ViewModel performs cleanup

### Exit PiP:
- [x] Exit PiP → UI restored to normal
- [x] Player continues playing
- [x] PiP button reappears

---

## Summary

| Metric | Proposed | Actual | Status |
|--------|----------|--------|--------|
| Imports added per fragment | ~10 | ~5-6 | ✅ Fewer (cleaner) |
| Properties added per fragment | 2 | 2-3 | ✅ Match (+ytController for YouTube) |
| Lifecycle methods modified | 4 | 4-5 | ✅ Match (+onResume for permission) |
| New methods per fragment | 7 | 0 new methods (reused existing) | ✅ Better — no method proliferation |
| DI entries | 5 | 4 | ✅ Simpler |
| Lines added per fragment | ~150-200 | ~50-80 (integration code only) | ✅ Much leaner |
| Fragments modified | 2 | 2 | ✅ Match |

**Overall: The actual implementation meets all requirements from `pip_integration_changes.md` with a cleaner, leaner approach.**

---

**Document Version:** 1.0  
**Last Updated:** 13 May 2026
