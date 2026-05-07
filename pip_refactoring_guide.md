# PiP Refactoring Guide - REVISED (Reuse Existing Fragments)

**Objective:** Integrate PiP functionality directly into existing `VideoUnitFragment` and `YoutubeVideoUnitFragment`
**Approach:** Minimal changes to existing fragments, add PiP capabilities in-place
**Advantage:** No new fragment classes, less code duplication, simpler migration

---

## Architecture Overview (Revised)

```
Existing Fragments (REUSED):
    ├── VideoUnitFragment (ExoPlayer)
    │   ├── + Add pipViewModel injection
    │   ├── + Add PiP state observers
    │   ├── + Add PiP button handler
    │   └── + Manage BroadcastReceiver lifecycle
    │
    └── YoutubeVideoUnitFragment (YouTube)
        ├── + Add pipViewModel injection
        ├── + Add PiP state observers
        ├── + Add PiP button handler
        └── + Manage BroadcastReceiver lifecycle

Domain Layer (NEW):
    ├── PipInteractor (business logic)
    └── PipModel (domain models)

Data Layer (NEW):
    ├── PipPlayerRepository (player management)
    ├── PipBroadcastReceiverManager (receiver lifecycle)
    └── PlayerController abstraction

DI Layer (New entries in ScreenModule):
    ├── PipPlayerRepository
    ├── PipInteractor
    └── PipViewModel
```

---

## TIER 1: DATA LAYER (Foundation)

### Same as Before
All Tier 1 files remain identical:
- `PlayerController.kt` (abstraction)
- `PipPlayerState.kt` (domain models)
- `PipPlayerRepository.kt` (repository)
- `PipBroadcastReceiverManager.kt` (receiver management)

---

## TIER 2: DOMAIN LAYER (Business Logic)

### Same as Before
All Tier 2 files remain identical:
- `PipInteractor.kt` (business logic)

---

## TIER 3: PRESENTATION LAYER (Modified Approach)

### Goal: Extend existing fragments with PiP capabilities

### 3.1 Create PiP ViewModel (SAME AS BEFORE)

**File:** `course/src/main/java/org/openedx/course/presentation/unit/video/PipViewModel.kt`

*(Copy from previous guide - no changes)*

---

### 3.2 REVISED: Enhance VideoUnitFragment (ExoPlayer)

**File:** `course/src/main/java/org/openedx/course/presentation/unit/video/VideoUnitFragment.kt`

Instead of creating `PipVideoUnitFragment`, add PiP methods directly to `VideoUnitFragment`:

```kotlin
class VideoUnitFragment : Fragment() {
    // ... existing properties ...

    // NEW: Activity-scoped ViewModel for PiP state
    private val pipViewModel by activityViewModels<PipViewModel>()

    // NEW: BroadcastReceiver manager for PiP actions
    private var pipReceiverManager: PipBroadcastReceiverManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ... existing code ...

        // NEW: Setup PiP observers
        setupPipObservers()
    }

    override fun onStart() {
        super.onStart()

        // ... existing code ...

        // NEW: Register PiP BroadcastReceiver
        pipReceiverManager?.register(viewLifecycleOwner.lifecycleScope)
    }

    override fun onStop() {
        super.onStop()

        // ... existing code ...

        // NEW: Unregister PiP BroadcastReceiver
        pipReceiverManager?.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()

        // ... existing code ...

        // NEW: Cleanup PiP state
        viewLifecycleOwner.lifecycleScope.launch {
            pipViewModel.onPipModeExited()
        }
    }

    // ============================================================
    // NEW: PiP-Related Methods - Added to existing fragment
    // ============================================================

    /**
     * Initialize PiP system when player is ready
     * Call from onExoPlayerReady() or similar
     */
    private fun initializePiPSystem(player: ExoPlayer) {
        // Initialize receiver manager
        pipReceiverManager = PipBroadcastReceiverManager(
            requireContext(),
            get<PipPlayerRepository>()
        )

        // Register player with PiP system
        viewLifecycleOwner.lifecycleScope.launch {
            pipViewModel.pipInteractor.registerPlayer(
                player,
                org.openedx.course.domain.model.PlayerType.EXOPLAYER,
            ) { exoPlayer ->
                org.openedx.course.data.repository.player.ExoPlayerController(
                    exoPlayer as ExoPlayer
                )
            }
        }

        // Listen to player state changes
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewLifecycleOwner.lifecycleScope.launch {
                    pipViewModel.updatePlaybackState(isPlaying)
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                viewLifecycleOwner.lifecycleScope.launch {
                    pipViewModel.updatePlayerPosition(
                        player.currentPosition,
                        player.duration
                    )
                }
            }
        })
    }

    /**
     * Handle PiP button click
     * Call from existing UI click handler
     */
    fun onPipButtonClicked() {
        viewLifecycleOwner.lifecycleScope.launch {
            val aspectRatio = org.openedx.course.domain.model.AspectRatio.RATIO_16_9
            pipViewModel.requestEnterPipMode(requireContext(), aspectRatio)
        }
    }

    /**
     * Setup observers for PiP state and actions
     */
    private fun setupPipObservers() {
        // Observe PiP state changes
        viewLifecycleOwner.lifecycleScope.launch {
            pipViewModel.pipState.collect { state ->
                updatePipUI(state)
            }
        }

        // Observe PiP action events
        viewLifecycleOwner.lifecycleScope.launch {
            pipViewModel.pipAction.collect { action ->
                action?.let { handlePipAction(it) }
            }
        }
    }

    /**
     * Update UI based on PiP state
     */
    private fun updatePipUI(state: org.openedx.course.domain.model.PipPlayerState) {
        if (state.isPipMode) {
            // Hide PiP button (we're already in PiP mode)
            hidePipButton()
        } else {
            // Show PiP button (available to enter PiP mode)
            showPipButton()
        }
    }

    /**
     * Handle PiP action events
     */
    private fun handlePipAction(action: PipActionEvent) {
        when (action) {
            is PipActionEvent.ShowPipHint -> {
                // Show hint: "Now in Picture-in-Picture mode"
                showSnackBar("Now in Picture-in-Picture mode")
            }
            is PipActionEvent.UpdateFullscreen -> {
                // Update fullscreen button state
                updateFullscreenButton(false)
            }
            is PipActionEvent.UpdatePlaybackUI -> {
                // Update play/pause button
                updatePlayPauseButton(action.isPlaying)
            }
            is PipActionEvent.UpdateProgress -> {
                // Update progress bar if visible
                updateProgressBar(action.position, action.duration)
            }
        }
    }

    private fun showPipButton() {
        // Existing method or add: pipButton.visibility = View.VISIBLE
    }

    private fun hidePipButton() {
        // Existing method or add: pipButton.visibility = View.GONE
    }

    private fun showSnackBar(message: String) {
        // Use existing snackbar logic
    }

    private fun updateFullscreenButton(isFullscreen: Boolean) {
        // Update fullscreen button state
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        // Update play/pause button based on state
    }

    private fun updateProgressBar(position: Long, duration: Long) {
        // Update progress bar if available
    }

    /**
     * Override onPictureInPictureModeChanged if needed
     */
    override fun onPictureInPictureModeChanged(isInPipMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPipMode, newConfig)
        if (!isInPipMode) {
            viewLifecycleOwner.lifecycleScope.launch {
                pipViewModel.onPipModeExited()
            }
        }
    }
}
```

**Integration Points:**
1. After `onExoPlayerReady()` or when player is created:
   ```kotlin
   initializePiPSystem(exoPlayer)
   ```

2. When PiP button is clicked in existing UI:
   ```kotlin
   onPipButtonClicked()
   ```

3. PiP state automatically observed in `setupPipObservers()` (called in `onViewCreated`)

---

### 3.3 REVISED: Enhance YoutubeVideoUnitFragment

**File:** `course/src/main/java/org/openedx/course/presentation/unit/video/YoutubeVideoUnitFragment.kt`

Same approach as ExoPlayer - add PiP methods directly:

```kotlin
class YoutubeVideoUnitFragment : Fragment() {
    // ... existing properties ...

    // NEW: Activity-scoped ViewModel for PiP state
    private val pipViewModel by activityViewModels<PipViewModel>()

    // NEW: BroadcastReceiver manager for PiP actions
    private var pipReceiverManager: PipBroadcastReceiverManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ... existing code ...

        // NEW: Setup PiP observers
        setupPipObservers()
    }

    override fun onStart() {
        super.onStart()

        // ... existing code ...

        // NEW: Register PiP BroadcastReceiver
        pipReceiverManager?.register(viewLifecycleOwner.lifecycleScope)
    }

    override fun onStop() {
        super.onStop()

        // ... existing code ...

        // NEW: Unregister PiP BroadcastReceiver
        pipReceiverManager?.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()

        // ... existing code ...

        // NEW: Cleanup PiP state
        viewLifecycleOwner.lifecycleScope.launch {
            pipViewModel.onPipModeExited()
        }
    }

    // ============================================================
    // NEW: PiP-Related Methods - Added to existing fragment
    // ============================================================

    /**
     * Initialize PiP system when YouTube player is ready
     * Call from onYoutubePlayerReady() or similar
     */
    private fun initializePiPSystem(player: YouTubePlayer) {
        // Initialize receiver manager
        pipReceiverManager = PipBroadcastReceiverManager(
            requireContext(),
            get<PipPlayerRepository>()
        )

        // Register player with PiP system
        viewLifecycleOwner.lifecycleScope.launch {
            pipViewModel.pipInteractor.registerPlayer(
                player,
                org.openedx.course.domain.model.PlayerType.YOUTUBE,
            ) { youtubePlayer ->
                org.openedx.course.data.repository.player.YouTubePlayerController(
                    youtubePlayer as YouTubePlayer
                )
            }
        }

        // Listen to YouTube player state changes
        player.setOnStateChangeListener { state ->
            viewLifecycleOwner.lifecycleScope.launch {
                val isPlaying = state == YouTubePlayer.PlayerState.PLAYING
                pipViewModel.updatePlaybackState(isPlaying)
            }
        }
    }

    /**
     * Handle PiP button click
     * Call from existing UI click handler
     */
    fun onPipButtonClicked() {
        viewLifecycleOwner.lifecycleScope.launch {
            val aspectRatio = org.openedx.course.domain.model.AspectRatio.RATIO_16_9
            pipViewModel.requestEnterPipMode(requireContext(), aspectRatio)
        }
    }

    /**
     * Setup observers for PiP state and actions
     */
    private fun setupPipObservers() {
        // Observe PiP state changes
        viewLifecycleOwner.lifecycleScope.launch {
            pipViewModel.pipState.collect { state ->
                updatePipUI(state)
            }
        }

        // Observe PiP action events
        viewLifecycleOwner.lifecycleScope.launch {
            pipViewModel.pipAction.collect { action ->
                action?.let { handlePipAction(it) }
            }
        }
    }

    /**
     * Update UI based on PiP state
     */
    private fun updatePipUI(state: org.openedx.course.domain.model.PipPlayerState) {
        if (state.isPipMode) {
            hidePipButton()
        } else {
            showPipButton()
        }
    }

    /**
     * Handle PiP action events
     */
    private fun handlePipAction(action: PipActionEvent) {
        when (action) {
            is PipActionEvent.ShowPipHint -> {
                showSnackBar("Now in Picture-in-Picture mode")
            }
            is PipActionEvent.UpdateFullscreen -> {
                updateFullscreenButton(false)
            }
            is PipActionEvent.UpdatePlaybackUI -> {
                updatePlayPauseButton(action.isPlaying)
            }
            is PipActionEvent.UpdateProgress -> {
                updateProgressBar(action.position, action.duration)
            }
        }
    }

    private fun showPipButton() {
        // Show PiP button
    }

    private fun hidePipButton() {
        // Hide PiP button
    }

    private fun showSnackBar(message: String) {
        // Show snackbar
    }

    private fun updateFullscreenButton(isFullscreen: Boolean) {
        // Update fullscreen button
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        // Update play/pause button
    }

    private fun updateProgressBar(position: Long, duration: Long) {
        // Update progress bar
    }

    /**
     * Override onPictureInPictureModeChanged if needed
     */
    override fun onPictureInPictureModeChanged(isInPipMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPipMode, newConfig)
        if (!isInPipMode) {
            viewLifecycleOwner.lifecycleScope.launch {
                pipViewModel.onPipModeExited()
            }
        }
    }
}
```

---

## TIER 4: DEPENDENCY INJECTION (Same as Before)

Add to `ScreenModule.kt`:

```kotlin
// Data Layer
single { PipPlayerRepository(get()) }
single { (pipRepository: PipPlayerRepository) ->
    PipBroadcastReceiverManager(get(), pipRepository)
}

// Domain Layer
factory { PipInteractor(get()) }

// Presentation Layer
viewModel {
    PipViewModel(
        pipInteractor = get()
    )
}
```

---

## TIER 5: TESTING (Same as Before)

- `PipPlayerRepositoryTest.kt`
- `PipInteractorTest.kt`
- `PipViewModelTest.kt`

*(No changes from original guide)*

---

## Migration Plan (SIMPLIFIED)

### Phase 1: Setup (Week 1)
- [ ] Create Data Layer classes (PlayerController, Repository, Models)
- [ ] Create Domain Layer (Interactor)
- [ ] Write unit tests for Data & Domain layers
- [ ] Add DI module definitions

### Phase 2: Integrate into Existing Fragments (Week 2)
- [ ] Add PiP imports and properties to `VideoUnitFragment`
- [ ] Add PiP methods to `VideoUnitFragment`
- [ ] Add PiP imports and properties to `YoutubeVideoUnitFragment`
- [ ] Add PiP methods to `YoutubeVideoUnitFragment`
- [ ] Integrate `initializePiPSystem()` calls in both fragments
- [ ] Wire up PiP button click handlers

### Phase 3: Testing & Integration (Week 2-3)
- [ ] Write ViewModel tests
- [ ] Run full test suite
- [ ] Integration testing (manual)
- [ ] Fix any edge cases

### Phase 4: Cleanup (Week 4)
- [ ] Delete old `PipPlayerController` singleton
- [ ] Delete old `YoutubePipActionReceiver`
- [ ] Verify no remaining global state
- [ ] Documentation update
- [ ] Code review & merge

---

## File Structure Summary (REVISED)

```
New Architecture (No new Fragment classes):

course/src/main/java/org/openedx/course/
├── data/repository/
│   ├── player/
│   │   └── PlayerController.kt (NEW)
│   ├── PipPlayerRepository.kt (NEW)
│   └── PipBroadcastReceiverManager.kt (NEW)
│
├── domain/
│   ├── model/
│   │   └── PipPlayerState.kt (NEW)
│   └── interactor/
│       └── PipInteractor.kt (NEW)
│
└── presentation/unit/video/
    ├── PipViewModel.kt (NEW)
    ├── VideoUnitFragment.kt (MODIFIED - PiP methods added)
    ├── YoutubeVideoUnitFragment.kt (MODIFIED - PiP methods added)
    └── [Delete old PipPlayerController & YoutubePipActionReceiver]

Tests:
├── data/
│   └── PipPlayerRepositoryTest.kt (NEW)
├── domain/
│   └── PipInteractorTest.kt (NEW)
└── presentation/
    └── PipViewModelTest.kt (NEW)
```

---

## Code Changes Summary

### VideoUnitFragment Changes

**Add these lines to imports:**
```kotlin
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openedx.course.presentation.unit.video.PipViewModel
import org.openedx.course.presentation.unit.video.PipActionEvent
import org.openedx.course.data.repository.PipBroadcastReceiverManager
import org.openedx.course.data.repository.PipPlayerRepository
import org.openedx.course.data.repository.player.ExoPlayerController
import org.openedx.course.domain.model.PlayerType
```

**Add these properties to class:**
```kotlin
private val pipViewModel by activityViewModels<PipViewModel>()
private var pipReceiverManager: PipBroadcastReceiverManager? = null
```

**Add method calls:**
- In `onViewCreated()`: Call `setupPipObservers()`
- In `onStart()`: Call `pipReceiverManager?.register(viewLifecycleOwner.lifecycleScope)`
- In `onStop()`: Call `pipReceiverManager?.unregister()`
- In `onDestroy()`: Call cleanup code
- When player ready: Call `initializePiPSystem(player)`
- When PiP button clicked: Call `onPipButtonClicked()`

**Add new methods:** (Copy from section 3.2 above)
- `initializePiPSystem()`
- `onPipButtonClicked()`
- `setupPipObservers()`
- `updatePipUI()`
- `handlePipAction()`
- `showPipButton()`
- `hidePipButton()`
- `showSnackBar()`
- `updateFullscreenButton()`
- `updatePlayPauseButton()`
- `updateProgressBar()`

### YoutubeVideoUnitFragment Changes

**Same as VideoUnitFragment** - replace ExoPlayer references with YouTube Player references

---

## Key Advantages of This Approach

✅ **No Fragment Inheritance** - Avoids deep class hierarchies
✅ **Reuses Existing Code** - Minimal duplication
✅ **Single Fragment Class** - All logic in one place
✅ **Easier to Debug** - Linear code flow
✅ **Less Files** - Simpler project structure
✅ **Easier Migration** - Incremental changes to existing files
✅ **Backwards Compatible** - Existing functionality unchanged
✅ **Same Architecture** - Still follows Koin DI and Clean Architecture

---

## Validation Checklist

- [ ] All dependencies injected via Koin
- [ ] No global singletons for state
- [ ] All state in ViewModel with StateFlow
- [ ] Lifecycle properly bound (lifecycleScope)
- [ ] ExoPlayer and YouTube use identical patterns
- [ ] All layers have unit tests
- [ ] No Android dependencies in Domain layer
- [ ] Repository is single source of truth
- [ ] BroadcastReceiver properly registered/unregistered
- [ ] Player properly cleaned up on destroy
- [ ] Code compiles and all tests pass
- [ ] PiP functionality integrated into existing fragments
- [ ] No new Fragment subclasses created
- [ ] Old singleton code deleted

---

**Document Version:** 1.1 (Revised - Reuse Existing Fragments)
**Status:** Ready for Implementation
**Estimated Effort:** 2-3 weeks (reduced from 3-4)
**Risk Level:** Very Low (isolated, minimal changes)
