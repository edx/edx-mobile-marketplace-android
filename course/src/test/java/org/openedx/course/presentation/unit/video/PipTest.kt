package org.openedx.course.presentation.unit.video

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.openedx.course.data.repository.PipPlayerRepository
import org.openedx.course.data.repository.player.PlayerController
import org.openedx.course.domain.interactor.PipInteractor
import org.openedx.course.domain.model.PipAction
import org.openedx.course.domain.model.PipPlayerState
import org.openedx.course.domain.model.PipPlayerType
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
/**
 * Unit tests for PiP refactored architecture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PipPlayerRepositoryTest {

    private lateinit var repository: PipPlayerRepository

    @Before
    fun setup() {
        repository = PipPlayerRepository()
    }

    @Test
    fun `initial state is default`() {
        val state = repository.state.value
        assertEquals(PipPlayerType.NONE, state.playerType)
        assertFalse(state.isPlaying)
        assertFalse(state.isEnded)
        assertFalse(state.isPipMode)
    }

    @Test
    fun `registerPlayer updates state`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns true
        every { controller.isEnded() } returns false

        repository.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        assertEquals(PipPlayerType.EXOPLAYER, repository.state.value.playerType)
        assertTrue(repository.state.value.isPlaying)
    }

    @Test
    fun `unregisterPlayer resets state`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns true
        every { controller.isEnded() } returns false
        repository.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        repository.unregisterPlayer()

        assertNull(repository.controller)
        assertEquals(PipPlayerState(), repository.state.value)
    }

    @Test
    fun `updatePlaybackState updates isPlaying`() {
        repository.updatePlaybackState(isPlaying = true)
        assertTrue(repository.state.value.isPlaying)

        repository.updatePlaybackState(isPlaying = false)
        assertFalse(repository.state.value.isPlaying)
    }

    @Test
    fun `enterPipMode and exitPipMode update state`() {
        repository.enterPipMode()
        assertTrue(repository.state.value.isPipMode)

        repository.exitPipMode()
        assertFalse(repository.state.value.isPipMode)
    }

    @Test
    fun `play delegates to controller`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns false
        every { controller.isEnded() } returns false
        repository.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        repository.play()

        verify { controller.play() }
    }

    @Test
    fun `pause delegates to controller`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns true
        every { controller.isEnded() } returns false
        repository.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        repository.pause()

        verify { controller.pause() }
    }

    @Test
    fun `seekForward delegates to controller`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns true
        every { controller.isEnded() } returns false
        repository.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        repository.seekForward()

        verify { controller.seekForward(10_000) }
    }

    @Test
    fun `seekBackward delegates to controller`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns true
        every { controller.isEnded() } returns false
        repository.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        repository.seekBackward()

        verify { controller.seekBackward(10_000) }
    }

    @Test
    fun `play does nothing when no controller registered`() {
        // Should not throw
        repository.play()
        repository.pause()
        repository.seekForward()
        repository.seekBackward()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PipInteractorTest {

    private lateinit var repository: PipPlayerRepository
    private lateinit var interactor: PipInteractor

    @Before
    fun setup() {
        repository = PipPlayerRepository()
        interactor = PipInteractor(repository)
    }

    @Test
    fun `registerPlayer updates state through interactor`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns false
        every { controller.isEnded() } returns false

        interactor.registerPlayer(controller, PipPlayerType.YOUTUBE)

        assertEquals(PipPlayerType.YOUTUBE, interactor.pipState.value.playerType)
    }

    @Test
    fun `handleAction Play delegates correctly`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns false
        every { controller.isEnded() } returns false
        interactor.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        interactor.handleAction(PipAction.Play)

        verify { controller.play() }
    }

    @Test
    fun `handleAction Pause delegates correctly`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns true
        every { controller.isEnded() } returns false
        interactor.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        interactor.handleAction(PipAction.Pause)

        verify { controller.pause() }
    }

    @Test
    fun `handleAction SeekForward delegates correctly`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns true
        every { controller.isEnded() } returns false
        interactor.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        interactor.handleAction(PipAction.SeekForward)

        verify { controller.seekForward(10_000) }
    }

    @Test
    fun `handleAction Replay delegates correctly`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns false
        every { controller.isEnded() } returns true
        interactor.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        interactor.handleAction(PipAction.Replay)

        verify { controller.restart() }
    }

    @Test
    fun `enterPipMode and exitPipMode update state`() {
        interactor.enterPipMode()
        assertTrue(interactor.pipState.value.isPipMode)

        interactor.exitPipMode()
        assertFalse(interactor.pipState.value.isPipMode)
    }

    @Test
    fun `hasPlayer returns false initially`() {
        assertFalse(interactor.hasPlayer())
    }

    @Test
    fun `hasPlayer returns true after registration`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns false
        every { controller.isEnded() } returns false
        interactor.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        assertTrue(interactor.hasPlayer())
    }

    @Test
    fun `unregisterPlayer clears state`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns false
        every { controller.isEnded() } returns false
        interactor.registerPlayer(controller, PipPlayerType.EXOPLAYER)

        interactor.unregisterPlayer()

        assertFalse(interactor.hasPlayer())
        assertEquals(PipPlayerType.NONE, interactor.pipState.value.playerType)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PipViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: PipPlayerRepository
    private lateinit var interactor: PipInteractor
    private lateinit var viewModel: PipViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = PipPlayerRepository()
        interactor = PipInteractor(repository)
        viewModel = PipViewModel(interactor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is default`() {
        val state = viewModel.pipState.value
        assertEquals(PipPlayerType.NONE, state.playerType)
        assertFalse(state.isPlaying)
        assertFalse(state.isPipMode)
    }

    @Test
    fun `exitPipMode updates state`() {
        viewModel.enterPipMode()
        viewModel.exitPipMode()
        assertFalse(viewModel.pipState.value.isPipMode)
    }

    @Test
    fun `unregisterPlayer resets state`() {
        val controller = mockk<PlayerController>(relaxed = true)
        every { controller.isPlaying() } returns true
        every { controller.isEnded() } returns false
        viewModel.registerPlayer(controller, PipPlayerType.YOUTUBE)

        viewModel.unregisterPlayer()

        assertEquals(PipPlayerType.NONE, viewModel.pipState.value.playerType)
        assertFalse(viewModel.pipState.value.isPlaying)
    }

}