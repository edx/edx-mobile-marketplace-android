package org.openedx.course.presentation.unit.video

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.module.TranscriptManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.core.utils.Logger
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent

@OptIn(ExperimentalCoroutinesApi::class)
class VideoUnitViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val courseRepository = mockk<CourseRepository>()
    private val notifier = mockk<CourseNotifier>()
    private val networkConnection = mockk<NetworkConnection>()
    private val transcriptManager = mockk<TranscriptManager>()
    private val courseAnalytics = mockk<CourseAnalytics>()


    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `CourseVideoPositionChanged notifier test`() = runTest {
        val viewModel = VideoUnitViewModel(
            "",
            "",
            courseRepository,
            notifier,
            networkConnection,
            transcriptManager,
            courseAnalytics,
        )
        coEvery { notifier.notifier } returns flow {
            emit(
                CourseVideoPositionChanged(
                    "",
                    10,
                    1000,
                    false
                )
            )
        }
        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        assert(viewModel.currentVideoTime.value == 10L)
    }

    @Test
    fun `markBlockCompleted success`() = runTest {
        val viewModel = VideoUnitViewModel(
            courseId = "test_course",
            blockId = "test_block",
            courseRepository,
            notifier,
            networkConnection,
            transcriptManager,
            courseAnalytics,
        )
        coEvery {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        } returns Unit
        every {
            courseAnalytics.logEvent(
                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
                any()
            )
        } returns Unit
        coEvery { notifier.send(any<CourseCompletionSet>()) } returns Unit

        viewModel.markBlockCompleted("test_block")
        advanceUntilIdle()

        // Verify completion was marked
        coVerify(exactly = 1) {
            courseRepository.markBlocksCompletion(
                "test_course",
                listOf("test_block")
            )
        }
        // Verify analytics event was logged
        verify(exactly = 1) {
            courseAnalytics.logEvent(
                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
                any()
            )
        }
        // Verify notifier was sent
        coVerify(exactly = 1) {
            notifier.send(any<CourseCompletionSet>())
        }
    }

//    @Test
//    fun `markBlockCompleted exception handling`() = runTest {
//        val viewModel = VideoUnitViewModel(
//            courseId = "test_course",
//            blockId = "test_block",
//            courseRepository,
//            notifier,
//            networkConnection,
//            transcriptManager,
//            courseAnalytics,
//        )
//        coEvery {
//            courseRepository.markBlocksCompletion(
//                any(),
//                any()
//            )
//        } throws Exception("Network error")
//        every {
//            courseAnalytics.logEvent(
//                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
//                any()
//            )
//        } returns Unit
//
//        viewModel.markBlockCompleted("test_block")
//        advanceUntilIdle()
//
//        // Verify completion was attempted
//        coVerify(exactly = 1) {
//            courseRepository.markBlocksCompletion(
//                "test_course",
//                listOf("test_block")
//            )
//        }
//        // Verify analytics event was still logged before exception
//        verify(exactly = 1) {
//            courseAnalytics.logEvent(
//                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
//                any()
//            )
//        }
//        // Verify notifier was NOT sent due to exception
//        coVerify(exactly = 0) {
//            notifier.send(any<CourseCompletionSet>())
//        }
//    }

    @Test
    fun `markBlockCompleted idempotency`() = runTest {
        val viewModel = VideoUnitViewModel(
            courseId = "test_course",
            blockId = "test_block",
            courseRepository,
            notifier,
            networkConnection,
            transcriptManager,
            courseAnalytics,
        )
        coEvery {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        } returns Unit
        every {
            courseAnalytics.logEvent(
                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
                any()
            )
        } returns Unit
        coEvery { notifier.send(any<CourseCompletionSet>()) } returns Unit

        // Call markBlockCompleted twice
        viewModel.markBlockCompleted("test_block")
        advanceUntilIdle()
        viewModel.markBlockCompleted("test_block")
        advanceUntilIdle()

        // Verify API was called only once (not twice)
        coVerify(exactly = 1) {
            courseRepository.markBlocksCompletion(
                "test_course",
                listOf("test_block")
            )
        }
        // Verify analytics was logged only once
        verify(exactly = 1) {
            courseAnalytics.logEvent(
                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
                any()
            )
        }
    }
}