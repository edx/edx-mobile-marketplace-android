package org.openedx.discussion.presentation.responses

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
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.UIMessage
import org.openedx.core.data.storage.CorePreferences
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.openedx.core.domain.model.Pagination
import org.openedx.core.utils.Logger
import org.openedx.discussion.R
import org.openedx.discussion.DiscussionMocks
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.CommentsData
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import org.openedx.core.R as CoreR

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionResponsesViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val preferencesManager = mockk<CorePreferences>()
    private val analytics = mockk<DiscussionAnalytics>()
    private val notifier = mockk<DiscussionNotifier>(relaxed = true)

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong. Please try again later."

    //region mockComment

    private val mockComment = DiscussionComment(
        id = "",
        author = "",
        authorLabel = "",
        createdAt = "",
        updatedAt = "",
        rawBody = "",
        renderedBody = "",
        parsedRenderedBody = LinkedImageText("", emptyMap(), emptyMap(), emptyList()),
        abuseFlagged = false,
        voted = true,
        voteCount = 20,
        editableFields = emptyList(),
        canDelete = false,
        threadId = "",
        parentId = "",
        endorsed = false,
        endorsedBy = "",
        endorsedByLabel = "",
        endorsedAt = "",
        childCount = 21,
        children = emptyList(),
        profileImage = null,
        users = emptyMap(),
        isAuthor = false,
    )

    //endregion


    private val comments = listOf(
        DiscussionMocks.comment.copy(id = "0"),
        DiscussionMocks.comment.copy(id = "1")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { analytics.logScreenEvent(any(), any()) } returns Unit
        every { preferencesManager.user?.username } returns ""
        coEvery { interactor.getRecaptchaToken("", any()) } returns ""
        every { resourceManager.getString(CoreR.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.discussion_something_went_wrong_error) } returns somethingWrong
        mockkConstructor(Logger::class)
        every { anyConstructed<Logger>().e(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadCommentResponses no internet connection exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } throws UnknownHostException()

        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Loading)
    }

    @Test
    fun `loadCommentResponses unknown exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } throws Exception()

        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Loading)
    }

    @Test
    fun `loadCommentResponses success with next page`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == true)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `loadCommentResponses success without next page`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `fetchMore not load`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `fetchMore load success`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), eq(1)) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        coEvery { interactor.getCommentsResponses(any(), eq(2)) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCommentsResponses(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `setCommentUpvoted no internet connection exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), eq(1)) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        coEvery { interactor.setCommentVoted(any(), any()) } throws UnknownHostException()
        viewModel.setCommentUpvoted("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
    }

    @Test
    fun `setCommentUpvoted unknown exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), eq(1)) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        coEvery { interactor.setCommentVoted(any(), any()) } throws Exception()
        viewModel.setCommentUpvoted("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
    }

    @Test
    fun `setCommentUpvoted success without comments`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
        )
        coEvery { interactor.setCommentVoted(any(), any()) } returns mockComment.copy(id = "0")
        every { analytics.logEvent(any(), any()) } returns Unit
        coEvery {
            interactor.setCommentVoted(
                any(),
                any()
            )
        } returns DiscussionMocks.comment.copy(id = "0")
        viewModel.updateCommentResponses()
        viewModel.setCommentUpvoted("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }
        verify { analytics.logEvent(any(), any()) }
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `setCommentUpvoted success with comments`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
        )
        coEvery { interactor.setCommentVoted(any(), any()) } returns mockComment.copy(id = "2")
        every { analytics.logEvent(any(), any()) } returns Unit
        coEvery {
            interactor.setCommentVoted(
                any(),
                any()
            )
        } returns DiscussionMocks.comment.copy(id = "2")
        viewModel.updateCommentResponses()
        viewModel.setCommentUpvoted("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }
        verify { analytics.logEvent(any(), any()) }
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `setCommentReported no internet connection exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
        )
        coEvery { interactor.setCommentFlagged(any(), any()) } throws UnknownHostException()
        viewModel.setCommentReported("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
    }

    @Test
    fun `setCommentReported unknown exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        coEvery { interactor.setCommentFlagged(any(), any()) } throws Exception()
        viewModel.setCommentReported("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
    }

    @Test
    fun `setCommentReported success without comments`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.comment.copy(id = "0")
            preferencesManager,
            analytics,
        )
        coEvery { interactor.setCommentFlagged(any(), any()) } returns mockComment.copy(id = "0")
        every { analytics.logEvent(any(), any()) } returns Unit
        coEvery { interactor.setCommentFlagged(any(), any()) } returns DiscussionMocks.comment.copy(
            id = "0"
        )
        viewModel.setCommentReported("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }
        verify { analytics.logEvent(any(), any()) }
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `setCommentReported success with comments`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
        )
        coEvery { interactor.setCommentFlagged(any(), any()) } returns mockComment.copy(id = "0")
        every { analytics.logEvent(any(), any()) } returns Unit

        viewModel.updateCommentResponses()
        viewModel.setCommentReported("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }
        verify { analytics.logEvent(any(), any()) }
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `createComment no internet connection exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
        )
        coEvery {
            interactor.createComment(
                any(),
                any(),
                any(),
                any(),
            )
        } throws UnknownHostException()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        Assert.assertEquals(noInternet, message?.message)

    }

    @Test
    fun `createComment unknown exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any(), any()) } throws Exception()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        Assert.assertEquals(somethingWrong, message?.message)
    }

    @Test
    fun `createComment success`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any(), any()) } returns mockComment
        coEvery { interactor.createComment(any(), any(), any()) } returns DiscussionMocks.comment

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any(), any()) }

        assert(viewModel.uiMessage.value != null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `sendCommentAdded DiscussionCommentAdded`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any(), any()) } returns mockComment
        coEvery { interactor.createComment(any(), any(), any()) } returns DiscussionMocks.comment
        every { preferencesManager.user?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `sendCommentAdded DiscussionResponseAdded`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            "",
            "",
            true,
            mockComment.copy(id = "0"),
            interactor,
            resourceManager,
            notifier,
            preferencesManager,
            analytics,
            DiscussionMocks.comment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any(), any()) } returns mockComment
        coEvery { interactor.createComment(any(), any(), any()) } returns DiscussionMocks.comment
        every { preferencesManager.user?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }
}
