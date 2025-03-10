package org.openedx.notifications.presentation.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.UIMessage
import org.openedx.core.extension.isNull
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.FullScreenStateView
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.crop
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.notifications.R
import org.openedx.notifications.domain.model.InboxSection
import org.openedx.notifications.domain.model.NotificationContent
import org.openedx.notifications.domain.model.NotificationItem
import org.openedx.notifications.utils.TextUtils
import java.util.Date

class NotificationsInboxFragment : Fragment() {

    private val viewModel by viewModel<NotificationsInboxViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val uiState by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.collectAsState(null)
                val canLoadMore by viewModel.canLoadMore.collectAsState()
                val refreshing by viewModel.isRefreshing.collectAsState()

                InboxView(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    canLoadMore = canLoadMore,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onMenuClick = {
                        viewModel.logInboxMenuClicked()
                    },
                    onMenuItemClick = { menuType ->
                        when (menuType) {
                            NotificationsMenuType.MARK_ALL_READ -> {
                                viewModel.markAllNotificationsAsRead()
                            }

                            NotificationsMenuType.SETTINGS -> {
                                viewModel.navigateToPushNotificationsSettings(requireActivity().supportFragmentManager)
                            }
                        }
                    },
                    onSwipeRefresh = {
                        viewModel.onRefreshNotifications()
                    },
                    onReloadNotifications = {
                        viewModel.onReloadNotifications()
                    },
                    paginationCallBack = {
                        viewModel.fetchMore()
                    },
                    markNotificationAsRead = { notification, inboxSection ->
                        viewModel.markNotificationAsRead(
                            fm = requireActivity().supportFragmentManager,
                            notification = notification,
                            inboxSection = inboxSection,
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun InboxView(
    windowSize: WindowSize,
    uiState: InboxUIState,
    uiMessage: UIMessage?,
    canLoadMore: Boolean,
    refreshing: Boolean,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuItemClick: (NotificationsMenuType) -> Unit,
    onSwipeRefresh: () -> Unit,
    onReloadNotifications: () -> Unit,
    paginationCallBack: () -> Unit,
    markNotificationAsRead: (notificationItem: NotificationItem, inboxSection: InboxSection) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val topBarWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier
                    .fillMaxWidth()
            )
        )
    }
    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                compact = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        )
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { onSwipeRefresh() },
    )
    val firstVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
    }
    val lastVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
    }
    val loadMoreTriggerThreshold = 4

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(
                modifier = topBarWidth,
                onBackClick = onBackClick,
                onMenuClick = onMenuClick,
                onMenuItemClick = onMenuItemClick,
            )

            Surface(
                color = MaterialTheme.appColors.background
            ) {
                Box(
                    modifier = Modifier.pullRefresh(pullRefreshState),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (uiState) {
                        is InboxUIState.Data -> {
                            LazyColumn(
                                modifier = Modifier
                                    .background(MaterialTheme.appColors.background)
                                    .then(contentWidth),
                                state = scrollState,
                            ) {
                                uiState.notifications.forEach { (section, items) ->
                                    if (items.isNotEmpty()) {
                                        item {
                                            SectionHeader(
                                                section = section,
                                            )
                                        }

                                        items(items) { item ->
                                            NotificationItemView(
                                                modifier = Modifier.clickable {
                                                    markNotificationAsRead(item, section)
                                                },
                                                item = item,
                                            )
                                        }

                                        item {
                                            Spacer(Modifier.height(24.dp))
                                        }
                                    }
                                }

                                if (canLoadMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                        }
                                    }
                                }

                                if (scrollState.shouldLoadMore(
                                        rememberedFirstIndex = firstVisibleIndex,
                                        rememberedLastIndex = lastVisibleIndex,
                                        threshold = loadMoreTriggerThreshold
                                    )
                                ) {
                                    paginationCallBack()
                                }
                            }
                        }

                        is InboxUIState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is InboxUIState.Fallback -> {
                            FullScreenStateView(
                                state = uiState.state,
                                onAction = onReloadNotifications,
                            )
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

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuItemClick: (NotificationsMenuType) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        BackBtn(
            modifier = Modifier.align(Alignment.CenterStart),
            tint = MaterialTheme.appColors.textPrimary,
            onBackClick = onBackClick
        )

        Text(
            modifier = Modifier
                .testTag("txt_inbox_title")
                .align(Alignment.Center),
            text = stringResource(R.string.notifications_notifications),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        NotificationsDropdownMenu(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            onMenuClick = onMenuClick,
            onItemClick = onMenuItemClick,
        )
    }
}

@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    section: InboxSection,
) {
    Row(
        modifier = modifier.padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = section.titleResId),
            modifier = Modifier,
            style = MaterialTheme.appTypography.titleSmall,
            color = MaterialTheme.appColors.textPrimaryVariant,
        )
    }
}

@Composable
private fun NotificationsDropdownMenu(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    onItemClick: (NotificationsMenuType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    onMenuClick()
                    expanded = true
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                tint = MaterialTheme.appColors.primary,
                contentDescription = stringResource(id = R.string.notifications_accessibility_settings)
            )
        }

        MaterialTheme(
            colors = MaterialTheme.colors.copy(surface = MaterialTheme.appColors.background),
            shapes = MaterialTheme.shapes.copy(MaterialTheme.appShapes.material.medium)
        ) {
            DropdownMenu(
                modifier = Modifier
                    .crop(vertical = 8.dp)
                    .widthIn(min = 182.dp)
                    .background(MaterialTheme.appColors.surface),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                for (menuItem in NotificationsMenuType.entries) {
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.surface),
                        onClick = {
                            onItemClick(menuItem)
                            expanded = false
                        }
                    ) {
                        Text(
                            text = stringResource(id = menuItem.title),
                            style = MaterialTheme.appTypography.titleSmall,
                            color = MaterialTheme.appColors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemView(
    modifier: Modifier = Modifier,
    item: NotificationItem,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Forum,
            contentDescription = null,
            tint = MaterialTheme.appColors.textPrimary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = TextUtils.htmlContentToAnnotatedString(item),
                    style = MaterialTheme.appTypography.bodyMedium,
                    color = MaterialTheme.appColors.textPrimary,
                )
                if (item.lastRead.isNull()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .align(Alignment.CenterVertically)
                            .size(8.dp)
                            .background(MaterialTheme.appColors.primaryButtonBackground)
                    )
                } else {
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
            Text(
                text = TextUtils.dateToRelativeTimeString(LocalContext.current, item.created),
                style = MaterialTheme.appTypography.bodySmall,
                color = MaterialTheme.appColors.inboxTimeMarkerColor,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InboxPreview(
    @PreviewParameter(InboxUiStatePreviewParameterProvider::class) uiState: InboxUIState,
) {
    OpenEdXTheme {
        InboxView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = uiState,
            uiMessage = null,
            canLoadMore = true,
            refreshing = true,
            onBackClick = { },
            onMenuClick = { },
            onMenuItemClick = { },
            onSwipeRefresh = { },
            onReloadNotifications = { },
            paginationCallBack = { },
            markNotificationAsRead = { _, _ -> },
        )
    }
}

private val mockNotificationItem = NotificationItem(
    id = 0,
    appName = "",
    notificationType = "",
    contentUrl = "",
    created = Date(),
    courseId = "",
    lastRead = null,
    lastSeen = null,
    content = "Mock Content",
    contentContext = NotificationContent(
        paragraph = "",
        strongText = "",
        topicId = "",
        parentId = "",
        threadId = "",
        commentId = "",
        postTitle = "Mock",
        courseName = "",
        replierName = "",
        emailContent = "",
        authorName = "",
        authorPronoun = "",
    )
)

private class InboxUiStatePreviewParameterProvider : PreviewParameterProvider<InboxUIState> {
    override val values = sequenceOf(
        InboxUIState.Data(
            notifications = mapOf(
                InboxSection.RECENT to listOf(
                    mockNotificationItem,
                    mockNotificationItem
                ),
                InboxSection.OLDER to listOf(
                    mockNotificationItem,
                    mockNotificationItem
                )
            )
        ),
        InboxUIState.Fallback(state = InboxFullScreenState.Empty),
        InboxUIState.Fallback(state = InboxFullScreenState.NetworkError),
        InboxUIState.Fallback(state = InboxFullScreenState.ServerError),
    )
}
