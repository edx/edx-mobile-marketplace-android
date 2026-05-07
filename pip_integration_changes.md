# PiP Integration - Detailed Change Summary

Quick reference for modifying existing fragments to add PiP support.

---

## VideoUnitFragment.kt - Changes Required

### 1. Add Imports (Top of file)

```kotlin
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.openedx.course.presentation.unit.video.PipViewModel
import org.openedx.course.presentation.unit.video.PipActionEvent
import org.openedx.course.data.repository.PipBroadcastReceiverManager
import org.openedx.course.data.repository.PipPlayerRepository
import org.openedx.course.data.repository.player.ExoPlayerController
import org.openedx.course.domain.model.PlayerType
import org.openedx.course.domain.model.PipPlayerState
```

### 2. Add Properties (In class declaration)

```kotlin
class VideoUnitFragment : Fragment() {
    // ... existing properties ...

    // NEW: PiP ViewModel (Activity-scoped)
    private val pipViewModel by activityViewModels<PipViewModel>()

    // NEW: PiP receiver manager
    private var pipReceiverManager: PipBroadcastReceiverManager? = null
```

### 3. Modify onViewCreated()

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // ... existing code ...

    // NEW: Add this at the end
    setupPipObservers()
}
```

### 4. Modify onStart()

```kotlin
override fun onStart() {
    super.onStart()

    // ... existing code ...

    // NEW: Add this at the end
    pipReceiverManager?.register(viewLifecycleOwner.lifecycleScope)
}
```

### 5. Modify onStop()

```kotlin
override fun onStop() {
    super.onStop()

    // ... existing code ...

    // NEW: Add this at the end
    pipReceiverManager?.unregister()
}
```

### 6. Modify onDestroy()

```kotlin
override fun onDestroy() {
    super.onDestroy()

    // ... existing code ...

    // NEW: Add this at the end
    viewLifecycleOwner.lifecycleScope.launch {
        pipViewModel.onPipModeExited()
    }
}
```

### 7. Add Method: onExoPlayerCreated or onPlayerInitialized

Find where player is created/initialized. Add this call:

```kotlin
// When player is ready:
initializePiPSystem(exoPlayer)
```

### 8. Add New Methods (At end of class)

```kotlin
    // ============================================================
    // PiP-Related Methods
    // ============================================================

    /**
     * Initialize PiP system when player is ready
     */
    private fun initializePiPSystem(player: ExoPlayer) {
        // Initialize receiver manager
        pipReceiverManager = PipBroadcastReceiverManager(
            requireContext(),
            get<PipPlayerRepository>()
        )

        // Register player with PiP system
        viewLifecycleOwner.lifecycleScope.launch {
            val controller = ExoPlayerController(player)
            pipViewModel.pipInteractor.registerPlayer(
                player,
                PlayerType.EXOPLAYER,
            ) { controller }
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
     * Call from UI click handler: pipButton.setOnClickListener { onPipButtonClicked() }
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
    private fun updatePipUI(state: PipPlayerState) {
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
                // Toast or snackbar
            }
            is PipActionEvent.UpdateFullscreen -> {
                // Update fullscreen button state
            }
            is PipActionEvent.UpdatePlaybackUI -> {
                // Update play/pause button
            }
            is PipActionEvent.UpdateProgress -> {
                // Update progress bar
            }
        }
    }

    private fun showPipButton() {
        // Show PiP button in UI
    }

    private fun hidePipButton() {
        // Hide PiP button
    }

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

## YoutubeVideoUnitFragment.kt - Changes Required

Same as `VideoUnitFragment` above, with these changes:

### 1. Imports (Same as ExoPlayer)

### 2. Properties (Same as ExoPlayer)

### 3. Lifecycle methods (Same as ExoPlayer)

### 4. Player Initialization (DIFFERENT)

In `initializePiPSystem()`, use YouTube imports:

```kotlin
private fun initializePiPSystem(player: YouTubePlayer) {
    // Initialize receiver manager
    pipReceiverManager = PipBroadcastReceiverManager(
        requireContext(),
        get<PipPlayerRepository>()
    )

    // Register player with PiP system
    viewLifecycleOwner.lifecycleScope.launch {
        val controller = YouTubePlayerController(player)
        pipViewModel.pipInteractor.registerPlayer(
            player,
            PlayerType.YOUTUBE,
        ) { controller }
    }

    // Listen to YouTube player state changes
    player.setOnStateChangeListener { state ->
        viewLifecycleOwner.lifecycleScope.launch {
            val isPlaying = state == YouTubePlayer.PlayerState.PLAYING
            pipViewModel.updatePlaybackState(isPlaying)
        }
    }
}
```

### 5. All Other Methods (Same as ExoPlayer)

---

## Summary of Changes

| Item | Count |
|------|-------|
| **Import statements** | ~10 new imports |
| **Properties added** | 2 new properties |
| **Lifecycle methods modified** | 4 (onViewCreated, onStart, onStop, onDestroy) |
| **New methods added** | 7 new methods |
| **Lines of code added (per fragment)** | ~150-200 lines |
| **Fragments modified** | 2 (VideoUnitFragment, YoutubeVideoUnitFragment) |
| **New fragment files created** | 0 (REVISED - reuse existing) |

---

## Implementation Checklist

### For VideoUnitFragment:
- [ ] Add imports
- [ ] Add pipViewModel property
- [ ] Add pipReceiverManager property
- [ ] Add setupPipObservers() call in onViewCreated
- [ ] Add pipReceiverManager register call in onStart
- [ ] Add pipReceiverManager unregister call in onStop
- [ ] Add cleanup call in onDestroy
- [ ] Add initializePiPSystem() call when player is ready
- [ ] Add PiP button click handler
- [ ] Add all 7 new PiP methods
- [ ] Wire up PiP button in UI (onClick listener)

### For YoutubeVideoUnitFragment:
- [ ] Repeat all steps from VideoUnitFragment
- [ ] Use YouTube Player-specific code in initializePiPSystem()
- [ ] Use YouTubePlayerController instead of ExoPlayerController

### DI Module:
- [ ] Add 5 entries to ScreenModule.kt (see section 4 of main guide)

### Testing:
- [ ] Create PipPlayerRepositoryTest.kt
- [ ] Create PipInteractorTest.kt
- [ ] Create PipViewModelTest.kt
- [ ] Run all tests

### Cleanup:
- [ ] Delete old PipPlayerController.kt
- [ ] Delete old YoutubePipActionReceiver.kt
- [ ] Verify no other references to old classes

---

## Potential Integration Points in Existing Code

### VideoUnitFragment likely has:

1. **Player initialization location:**
   - Look for: `onPlayerReady()`, `onExoPlayerCreated()`, `setupExoPlayer()`
   - Add: `initializePiPSystem(exoPlayer)`

2. **PiP button location:**
   - Look for: `findViewById(R.id.pip_button)` or similar
   - Add: `.setOnClickListener { onPipButtonClicked() }`

3. **UI elements to update:**
   - PiP button visibility
   - Play/pause button state
   - Progress bar (if in PiP mode)
   - Fullscreen button state

### YoutubeVideoUnitFragment likely has:

1. **YouTube player initialization:**
   - Look for: `onYoutubePlayerReady()`, `setupYoutubePlayer()`
   - Add: `initializePiPSystem(youtubePlayer)`

2. **Same UI integration points as ExoPlayer**

---

## Testing the Integration

### Manual Testing Checklist:

1. **Basic PiP Entry:**
   - [ ] Click PiP button
   - [ ] Fragment enters PiP mode
   - [ ] Verify hint message shows

2. **Player Controls in PiP:**
   - [ ] Play/pause works via PendingIntent
   - [ ] Controls appear in PiP notifications

3. **State Management:**
   - [ ] Playback state updates reflect in UI
   - [ ] Position updates are tracked
   - [ ] Aspect ratio changes work

4. **Lifecycle Safety:**
   - [ ] Navigate away from fragment
   - [ ] Return to fragment
   - [ ] Player state is preserved
   - [ ] No crashes or memory leaks

5. **Configuration Changes:**
   - [ ] Rotate device during PiP
   - [ ] State is maintained

6. **Exit PiP:**
   - [ ] Exit PiP mode
   - [ ] UI returns to fullscreen
   - [ ] Player continues playing

---

## Common Issues & Solutions

### Issue: "pipViewModel not recognized"
**Solution:** Ensure PipViewModel is registered in ScreenModule.kt's viewModel block

### Issue: "Cannot find PipPlayerRepository"
**Solution:** Ensure PipPlayerRepository is registered as `single` in ScreenModule.kt

### Issue: "BroadcastReceiver not working"
**Solution:** Verify PipBroadcastReceiverManager is initialized before registering

### Issue: "Fragment crashes on PiP entry"
**Solution:** Check AndroidManifest.xml has `android:supportsPictureInPicture="true"` on activity

### Issue: "Tests fail with 'cannot mock pipViewModel'"
**Solution:** Use `activityViewModels()` with mockk fixtures, or use TestActivity

---

**Version:** 1.0
**Complexity:** Low (isolated changes to 2 files)
**Risk:** Very Low (backward compatible, no breaking changes)
