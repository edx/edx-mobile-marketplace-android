package org.openedx.notifications.presentation.inbox

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.extension.isNull
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
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
                val canLoadMore by viewModel.canLoadMore.collectAsState()

                InboxView(
                    windowSize = windowSize,
                    uiState = uiState,
                    canLoadMore = canLoadMore,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onSettingsClick = {

                    },
                    paginationCallBack = {
                        viewModel.fetchMore()
                    },
                )
            }
        }
    }
}

@Composable
private fun InboxView(
    windowSize: WindowSize,
    uiState: InboxUIState,
    canLoadMore: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    paginationCallBack: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
    }
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
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->
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
                onSettingsClick = onSettingsClick,
            )

            Surface(
                modifier = contentWidth,
                color = MaterialTheme.appColors.background
            ) {
                when (uiState) {
                    is InboxUIState.Data -> {
                        LazyColumn(
                            Modifier
                                .weight(1f)
                                .background(MaterialTheme.appColors.background),
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
                                        NotificationItemView(item = item)
                                    }

                                    item {
                                        Spacer(Modifier.height(24.dp))
                                    }
                                }
                            }

                            if (canLoadMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                    }
                                }
                            }

                            if (scrollState.shouldLoadMore(firstVisibleIndex, 4)) {
                                paginationCallBack()
                            }
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
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

        IconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = { onSettingsClick() }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                tint = MaterialTheme.appColors.primary,
                contentDescription = stringResource(id = R.string.notifications_accessibility_settings)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    section: InboxSection,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = section.titleResId),
            modifier = Modifier.padding(bottom = 24.dp),
            style = MaterialTheme.appTypography.titleSmall,
            color = MaterialTheme.appColors.textPrimaryVariant,
        )
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
                    text = item.content,
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
                }
            }
            Text(
                text = TextUtils.dateToRelativeTimeString(LocalContext.current, item.created),
                style = MaterialTheme.appTypography.bodySmall,
                color = MaterialTheme.appColors.notificationTimestamp,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun InboxPreview() {
    OpenEdXTheme {
        InboxView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = InboxUIState.Data(
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
            canLoadMore = true,
            onBackClick = { },
            onSettingsClick = { },
            paginationCallBack = { },
        )
    }
}

private val mockNotificationItem = NotificationItem(
    id = 0,
    appName = "",
    notificationType = "",
    contentUrl = "",
    created = Date(),
    lastRead = null,
    lastSeen = null,
    content = AnnotatedString("Mock Content"),
    contentContext = NotificationContent(
        paragraph = "",
        strongText = "",
        topicId = "",
        parentId = "",
        threadId = "",
        commentId = "",
        postTitle = "",
        courseName = "",
        replierName = "",
        emailContent = "",
    )
)
