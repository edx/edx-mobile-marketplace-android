package org.openedx.discussion.presentation.threads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.extension.TextConverter
import org.openedx.core.system.ResourceManager
import org.openedx.core.utils.Logger
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.domain.model.Thread
import org.openedx.discussion.domain.model.Topic
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadAdded
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionAddThreadViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val analytics = mockk<DiscussionAnalytics>()
    private val notifier = mockk<DiscussionNotifier>(relaxed = true)

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    //region mockThread

    private val mockThread = Thread(
        id = "",
        author = "",
        authorLabel = "",
        createdAt = "",
        updatedAt = "",
        rawBody = "",
        renderedBody = "",
        parsedRenderedBody = TextConverter.textToLinkedImageText(""),
        abuseFlagged = false,
        voted = true,
        voteCount = 20,
        editableFields = emptyList(),
        canDelete = false,
        courseId = "",
        topicId = "",
        groupId = "",
        groupName = "",
        type = DiscussionType.DISCUSSION,
        previewBody = "",
        abuseFlaggedCount = "",
        title = "Discussion title long Discussion title long good item",
        pinned = true,
        closed = false,
        following = true,
        commentCount = 21,
        unreadCommentCount = 4,
        read = false,
        hasEndorsed = false,
        users = mapOf(),
        responseCount = 0,
        anonymous = false,
        anonymousToPeers = false,
        isAuthor = false,
    )

    //endregion

    //region mockTopic

    private val mockTopic = Topic(
        id = "",
        name = "All Topics",
        threadListUrl = "",
        children = emptyList()
    )

    private val topics = listOf(
        mockTopic.copy(id = "0"),
        mockTopic.copy(id = "1"),
        mockTopic.copy(id = "2")
    )

    //endregion

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        mockkConstructor(Logger::class)
        every { anyConstructed<Logger>().e(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `createThread no internet connection exception`() = runTest {
        val viewModel =
            DiscussionAddThreadViewModel("", interactor, resourceManager, notifier, analytics)
        coEvery {
            interactor.createThread(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } throws UnknownHostException()

        viewModel.createThread("", "", "", "")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createThread(any(), any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.newThread.value == null)
        assert(viewModel.isLoading.value == false)
    }

    @Test
    fun `createThread unknown exception`() = runTest {
        val viewModel =
            DiscussionAddThreadViewModel("", interactor, resourceManager, notifier, analytics)
        coEvery {
            interactor.createThread(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } throws Exception()

        viewModel.createThread("", "", "", "")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createThread(any(), any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.newThread.value == null)
        assert(viewModel.isLoading.value == false)
    }

    @Test
    fun `createThread success`() = runTest {
        val viewModel =
            DiscussionAddThreadViewModel("", interactor, resourceManager, notifier, analytics)
        coEvery {
            interactor.createThread(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockThread
        every { analytics.logEvent(any(), any()) } returns Unit
        viewModel.createThread("", "", "", "")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createThread(any(), any(), any(), any(), any()) }
        verify { analytics.logEvent(any(), any()) }
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.newThread.value != null)
        assert(viewModel.isLoading.value == false)
    }

    @Test
    fun `sendThreadAdded notifier test`() = runTest {
        val viewModel =
            DiscussionAddThreadViewModel("", interactor, resourceManager, notifier, analytics)
        coEvery { notifier.send(mockk<DiscussionThreadAdded>()) }
        viewModel.sendThreadAdded()
        advanceUntilIdle()
    }

    @Test
    fun `getHandledTopicById existed id`() = runTest {
        val viewModel =
            DiscussionAddThreadViewModel("", interactor, resourceManager, notifier, analytics)
        coEvery { interactor.getCachedTopics(any()) } returns topics

        advanceUntilIdle()

        assert(viewModel.getHandledTopicById("1").second == "1")
    }

    @Test
    fun `getHandledTopicById  no existed id`() = runTest {
        val viewModel =
            DiscussionAddThreadViewModel("", interactor, resourceManager, notifier, analytics)
        coEvery { interactor.getCachedTopics(any()) } returns topics

        advanceUntilIdle()

        assert(viewModel.getHandledTopicById("10").second == "0")
    }


}
