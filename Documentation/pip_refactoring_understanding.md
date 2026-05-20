# PiP Refactoring Guide — Understanding & General Notes

## Summary

The guide describes integrating **Picture-in-Picture (PiP)** functionality into an existing Android app (Open edX) by extending the current `VideoUnitFragment` (ExoPlayer) and `YoutubeVideoUnitFragment` (YouTube) rather than creating new fragment subclasses.

---

## Key Architectural Decisions

1. **No new Fragment classes** — PiP logic is added directly into existing fragments. This avoids inheritance complexity and keeps the codebase flat.
2. **Clean Architecture layers** — New code is split into Data, Domain, and Presentation layers:
   - **Data:** `PlayerController` (abstraction), `PipPlayerRepository`, `PipBroadcastReceiverManager`
   - **Domain:** `PipInteractor`, `PipPlayerState` (models)
   - **Presentation:** `PipViewModel` (shared via `activityViewModels`)
3. **Koin DI** — All new classes are wired through `ScreenModule.kt` using Koin (`single`, `factory`, `viewModel`).
4. **State management** — All PiP state lives in `PipViewModel` using `StateFlow`, no global singletons.

---

## General Notes & Observations

### Architecture
- The `PipViewModel` is **activity-scoped** (`activityViewModels`), meaning it survives fragment recreation and is shared across fragments in the same activity. This is critical for PiP since the activity persists while the window shrinks.
- `PipPlayerRepository` acts as the **single source of truth** for player state, decoupling the player implementation from the PiP logic.
- `PlayerController` is an abstraction over ExoPlayer and YouTube Player, allowing the repository/interactor to control playback without knowing the concrete player type.

### Fragment Integration Pattern
- Both fragments follow an **identical pattern**: inject `pipViewModel`, create `PipBroadcastReceiverManager`, observe state, handle actions. The only difference is the player type (`ExoPlayer` vs `YouTubePlayer`) passed to `initializePiPSystem()`.
- Lifecycle hooks are used correctly:
  - `onViewCreated` → setup observers
  - `onStart` → register BroadcastReceiver
  - `onStop` → unregister BroadcastReceiver
  - `onDestroy` → cleanup PiP state
  - `onPictureInPictureModeChanged` → handle exit

### BroadcastReceiver
- `PipBroadcastReceiverManager` handles the media action buttons (play/pause, seek) that appear in the PiP window. It's registered/unregistered with lifecycle to avoid leaks.

### Potential Concerns
- **Code duplication** — The PiP methods added to both fragments are nearly identical. A shared helper/delegate class could reduce this, but the guide intentionally keeps it simple.
- **`onDestroy` usage** — The guide uses `viewLifecycleOwner.lifecycleScope` inside `onDestroy`, which may crash since the view lifecycle may already be destroyed at that point. Should use the fragment's own `lifecycleScope` there.
- **Direct interactor access** — `pipViewModel.pipInteractor.registerPlayer(...)` breaks ViewModel encapsulation. The ViewModel should expose a `registerPlayer()` method instead of leaking the interactor.
- **Fully qualified class names** — The code samples use long fully-qualified names (e.g., `org.openedx.course.domain.model.PlayerType`). In real code, these would be imports.

### Migration
- The plan is phased over ~3 weeks: foundation first (data/domain), then fragment integration, then testing, then cleanup of old singleton code (`PipPlayerController`, `YoutubePipActionReceiver`).
- Old global state is deleted in the final phase, which is good — allows rollback if needed.

### Testing
- Unit tests are planned for all three layers (`PipPlayerRepositoryTest`, `PipInteractorTest`, `PipViewModelTest`). No integration/UI tests mentioned beyond manual testing.

---

## Quick Reference: New Files to Create

| File | Layer | Type |
|------|-------|------|
| `PlayerController.kt` | Data | Interface |
| `PipPlayerRepository.kt` | Data | Repository |
| `PipBroadcastReceiverManager.kt` | Data | Lifecycle helper |
| `PipPlayerState.kt` | Domain | Data models |
| `PipInteractor.kt` | Domain | Business logic |
| `PipViewModel.kt` | Presentation | ViewModel |

## Quick Reference: Files to Modify

| File | Change |
|------|--------|
| `VideoUnitFragment.kt` | Add PiP properties, lifecycle hooks, methods |
| `YoutubeVideoUnitFragment.kt` | Add PiP properties, lifecycle hooks, methods |
| `ScreenModule.kt` | Add Koin definitions for PiP classes |

## Quick Reference: Files to Delete

| File | Reason |
|------|--------|
| `PipPlayerController` (old) | Replaced by repository pattern |
| `YoutubePipActionReceiver` (old) | Replaced by `PipBroadcastReceiverManager` |

---

**Document Version:** 1.0
**Based on:** pip_refactoring_guide.md v1.1
