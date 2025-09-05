package org.openedx.discussion.presentation.comments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.extension.TextConverter
import org.openedx.core.extension.parcelable
import org.openedx.core.extension.smoothScrollToIndex
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.discussion.R
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.domain.model.Thread
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.ui.CommentItem
import org.openedx.discussion.presentation.ui.ThreadMainItem


class DiscussionCommentsFragment : Fragment() {

    private val viewModel by viewModel<DiscussionCommentsViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().parcelable(ARG_THREAD)!!,
            requireArguments().getString(ARG_RESPONSE_ID, ""),
            requireArguments().getString(ARG_COMMENT_ID, ""),
            requireArguments().getBoolean(ARG_IS_POSTING_ENABLED, true),
        )
    }
    private val router by inject<DiscussionRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(DiscussionCommentsUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val showProgress by viewModel.showProgress.collectAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)

                DiscussionCommentsScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    title = viewModel.title,
                    canLoadMore = canLoadMore,
                    showProgress = showProgress,
                    refreshing = refreshing,
                    isPostingEnabled = viewModel.isPostingEnabled,
                    onCommentPulseEnd = { comment ->
                        viewModel.updateCommentPulseStatus(comment = comment)
                    },
                    onSwipeRefresh = {
                        viewModel.updateThreadComments()
                    },
                    paginationCallBack = {
                        viewModel.fetchMore()
                    },
                    onItemClick = { action, id, bool ->
                        if (!viewModel.thread.closed) {
                            when (action) {
                                ACTION_UPVOTE_COMMENT -> viewModel.setCommentUpvoted(id, bool)
                                ACTION_UPVOTE_THREAD -> viewModel.setThreadUpvoted(bool)
                                ACTION_FOLLOW_THREAD -> viewModel.setThreadFollowed(bool)
                                ACTION_REPORT_COMMENT -> viewModel.setCommentReported(id, bool)
                                ACTION_REPORT_THREAD -> viewModel.setThreadReported(bool)
                            }
                        } else {
                            when (action) {
                                ACTION_REPORT_COMMENT -> viewModel.setCommentReported(id, bool)
                                ACTION_REPORT_THREAD -> viewModel.setThreadReported(bool)
                            }
                        }
                    },
                    onCommentClick = {
                        router.navigateToDiscussionResponses(
                            requireActivity().supportFragmentManager,
                            viewModel.courseId,
                            viewModel.thread.id,
                            it,
                            viewModel.thread.closed,
                            viewModel.isPostingEnabled,
                        )
                    },
                    onUserPhotoClick = { username ->
                        router.navigateToAnothersProfile(
                            requireActivity().supportFragmentManager, username
                        )
                    },
                    onAddResponseClick = {
                        viewModel.createComment(it)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
                var fromNotificationNavigation by rememberSaveable {
                    mutableStateOf(viewModel.responseId.isNotEmpty() && viewModel.commentId.isNotEmpty())
                }
                LaunchedEffect(uiState) {
                    if (uiState is DiscussionCommentsUIState.Success && fromNotificationNavigation) {
                        val commentsData =
                            (uiState as DiscussionCommentsUIState.Success).commentsData
                        commentsData.find { it.id == viewModel.responseId }?.let {
                            router.navigateToDiscussionResponses(
                                requireActivity().supportFragmentManager,
                                viewModel.courseId,
                                viewModel.thread.id,
                                it,
                                viewModel.thread.closed,
                                viewModel.isPostingEnabled,
                            )
                            fromNotificationNavigation = false
                        }
                    }
                }
            }
        }
        requireArguments().putString(ARG_RESPONSE_ID, "")
        requireArguments().putString(ARG_COMMENT_ID, "")
    }

    companion object {
        const val ACTION_UPVOTE_COMMENT = "action_upvote_comment"
        const val ACTION_REPORT_COMMENT = "action_report_comment"
        const val ACTION_UPVOTE_THREAD = "action_upvote_thread"
        const val ACTION_REPORT_THREAD = "action_report_thread"
        const val ACTION_FOLLOW_THREAD = "action_follow_thread"

        private const val ARG_COURSE_ID = "argCourseId"
        private const val ARG_THREAD = "argThread"
        private const val ARG_RESPONSE_ID = "argResponseId"
        private const val ARG_COMMENT_ID = "argCommentId"
        private const val ARG_IS_POSTING_ENABLED = "argIsPostingEnabled"

        fun newInstance(
            courseId: String,
            thread: Thread,
            responseId: String,
            commentId: String,
            isPostingEnabled: Boolean,
        ): DiscussionCommentsFragment {
            val fragment = DiscussionCommentsFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_THREAD to thread,
                ARG_RESPONSE_ID to responseId,
                ARG_COMMENT_ID to commentId,
                ARG_IS_POSTING_ENABLED to isPostingEnabled,
            )
            return fragment
        }
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DiscussionCommentsScreen(
    windowSize: WindowSize,
    uiState: DiscussionCommentsUIState,
    uiMessage: UIMessage?,
    title: String,
    canLoadMore: Boolean,
    showProgress: Boolean,
    refreshing: Boolean,
    isPostingEnabled: Boolean,
    onCommentPulseEnd: (DiscussionComment) -> Unit = {},
    onSwipeRefresh: () -> Unit,
    paginationCallBack: () -> Unit,
    onItemClick: (String, String, Boolean) -> Unit,
    onCommentClick: (DiscussionComment) -> Unit,
    onAddResponseClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onUserPhotoClick: (String) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var responseValue by rememberSaveable {
        mutableStateOf("")
    }

    val sendButtonAlpha = if (responseValue.isEmpty()) 0.3f else 1f
    var itemHeightPx by remember { mutableFloatStateOf(0f) }


    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        backgroundColor = MaterialTheme.appColors.background
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val paddingContent by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = 0.dp,
                    compact = 24.dp
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .then(screenWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .displayCutoutForLandscape(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BackBtn {
                        onBackClick()
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                        text = title,
                        color = MaterialTheme.appColors.textPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.appColors.background
            ) {
                Box(Modifier.pullRefresh(pullRefreshState)) {
                    when (uiState) {
                        is DiscussionCommentsUIState.Success -> {
                            var previousFirstId by remember { mutableStateOf<String?>(null) }
                            val currentFirstId = uiState.commentsData.firstOrNull()?.id
                            LaunchedEffect(currentFirstId) {
                                if (currentFirstId != null && currentFirstId != previousFirstId) {
                                    delay(100)
                                    scrollState.animateScrollToItem(0)
                                    previousFirstId = currentFirstId
                                }
                            }

                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LazyColumn(
                                    Modifier
                                        .then(screenWidth)
                                        .weight(1f)
                                        .displayCutoutForLandscape()
                                        .background(MaterialTheme.appColors.background),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    contentPadding = PaddingValues(bottom = 24.dp),
                                    state = scrollState
                                ) {
                                    item {
                                        ThreadMainItem(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.appColors.background)
                                                .padding(horizontal = paddingContent)
                                                .padding(top = 32.dp)
                                                .onGloballyPositioned { layout ->
                                                    itemHeightPx = layout.size.height.toFloat()
                                                },
                                            thread = uiState.thread,
                                            onClick = { action, bool ->
                                                onItemClick(action, uiState.thread.id, bool)
                                            },
                                            onUserPhotoClick = { username ->
                                                onUserPhotoClick(username)
                                            }
                                        )
                                    }
                                    if (uiState.commentsData.isNotEmpty()) {
                                        item {
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = paddingContent)
                                                    .padding(top = 24.dp, bottom = 4.dp),
                                                text = pluralStringResource(
                                                    id = R.plurals.discussion_responses_capitalized,
                                                    uiState.count,
                                                    uiState.count
                                                ),
                                                color = MaterialTheme.appColors.textPrimary,
                                                style = MaterialTheme.appTypography.titleLarge
                                            )
                                        }
                                    }
                                    items(uiState.commentsData) { comment ->
                                        CommentItem(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = paddingContent)
                                                .clickable {
                                                    onCommentClick(comment)
                                                },
                                            comment = comment,
                                            onCommentPulseEnd = onCommentPulseEnd,
                                            onClick = { action, commentId, bool ->
                                                onItemClick(action, commentId, bool)
                                            },
                                            onAddCommentClick = {
                                                onCommentClick(comment)
                                            },
                                            onUserPhotoClick = {
                                                onUserPhotoClick(comment.author)
                                            })
                                    }
                                    item {
                                        if (canLoadMore) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                            }
                                        }
                                    }
                                }
                                if (scrollState.shouldLoadMore(firstVisibleIndex, 4)) {
                                    paginationCallBack()
                                }
                                if (!isSystemInDarkTheme()) {
                                    Divider(color = MaterialTheme.appColors.cardViewBorder)
                                }
                                if (isPostingEnabled) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.appColors.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            Modifier
                                                .then(screenWidth)
                                                .heightIn(84.dp, Dp.Unspecified)
                                                .padding(top = 16.dp, bottom = 24.dp)
                                                .padding(horizontal = 24.dp)
                                                .displayCutoutForLandscape(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedTextField(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(36.dp, 80.dp),
                                                value = responseValue,
                                                onValueChange = { str ->
                                                    responseValue = str
                                                },
                                                shape = MaterialTheme.appShapes.buttonShape,
                                                textStyle = MaterialTheme.appTypography.labelLarge,
                                                maxLines = 3,
                                                placeholder = {
                                                    Text(
                                                        text = stringResource(id = R.string.discussion_add_response),
                                                        color = MaterialTheme.appColors.textFieldHint,
                                                        style = MaterialTheme.appTypography.labelLarge,
                                                    )
                                                },
                                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                                    backgroundColor = MaterialTheme.appColors.textFieldBackgroundVariant,
                                                    unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                                                    textColor = MaterialTheme.appColors.textFieldText
                                                ),
                                                enabled = !uiState.thread.closed
                                            )
                                            if (!showProgress) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .alpha(sendButtonAlpha)
                                                        .background(MaterialTheme.appColors.primaryButtonBackground)
                                                        .clickable {
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                            if (responseValue.isNotEmpty()) {
                                                                onAddResponseClick(responseValue.trim())
                                                                responseValue = ""
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        modifier = Modifier.padding(7.dp),
                                                        painter = painterResource(id = R.drawable.discussion_ic_send),
                                                        contentDescription = stringResource(id = R.string.discussion_add_response),
                                                        tint = MaterialTheme.appColors.primaryButtonText
                                                    )
                                                }
                                            } else {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(48.dp).padding(7.dp),
                                                    color = MaterialTheme.appColors.primary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            val scrollToIndex =
                                uiState.commentsData.indexOfFirst { comment -> comment.shouldHighlight }
                            LaunchedEffect(scrollToIndex) {
                                // add delay to allow the UI to be drawn before scrolling
                                delay(500)
                                if (scrollToIndex != -1) {
                                    scrollState.smoothScrollToIndex(
                                        index = scrollToIndex + 1,
                                        itemHeightPx = itemHeightPx
                                    )
                                }
                            }
                        }

                        is DiscussionCommentsUIState.Loading -> {
                            Box(
                                Modifier
                                    .fillMaxSize(), contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }
                    }
                    PullRefreshIndicator(
                        refreshing,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionCommentsScreenPreview() {
    OpenEdXTheme {
        DiscussionCommentsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DiscussionCommentsUIState.Success(
                mockThread,
                listOf(mockComment, mockComment),
                2
            ),
            uiMessage = null,
            title = "Test Screen",
            canLoadMore = false,
            showProgress = false,
            isPostingEnabled = false,
            paginationCallBack = {},
            onItemClick = { _, _, _ ->

            },
            onCommentClick = {},
            onAddResponseClick = {},
            onBackClick = {},
            refreshing = false,
            onSwipeRefresh = {},
            onUserPhotoClick = {},
        )
    }
}


@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionCommentsScreenTabletPreview() {
    OpenEdXTheme {
        DiscussionCommentsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = DiscussionCommentsUIState.Success(
                mockThread,
                listOf(mockComment, mockComment),
                2
            ),
            uiMessage = null,
            title = "Test Screen",
            canLoadMore = false,
            showProgress = false,
            isPostingEnabled = false,
            paginationCallBack = {},
            onItemClick = { _, _, _ ->

            },
            onCommentClick = {},
            onAddResponseClick = {},
            onBackClick = {},
            refreshing = false,
            onSwipeRefresh = {},
            onUserPhotoClick = {},
        )
    }
}

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
    responseCount = 10,
    anonymous = false,
    anonymousToPeers = false,
    isAuthor = false,
)

private val mockComment = DiscussionComment(
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
    threadId = "",
    parentId = "",
    endorsed = false,
    endorsedBy = "",
    endorsedByLabel = "",
    endorsedAt = "",
    childCount = 21,
    children = emptyList(),
    profileImage = ProfileImage("", "", "", "", false),
    users = mapOf(),
    isAuthor = false,
)
