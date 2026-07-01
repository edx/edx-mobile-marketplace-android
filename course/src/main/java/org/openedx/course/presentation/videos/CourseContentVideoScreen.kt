package org.openedx.course.presentation.videos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.Progress
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.R
import org.openedx.course.presentation.container.CourseContentVideoEmptyState
import org.openedx.course.presentation.home.CourseVideoItem
import org.openedx.course.presentation.outline.CourseProgress
import org.openedx.core.utils.VideoPreview
import org.openedx.course.presentation.ui.ShowDeleteVideoConfirmationDialog
import org.openedx.core.R as coreR

@Composable
fun CourseContentVideoScreen(
    windowSize: WindowSize,
    viewModel: CourseVideoViewModel,
    fragmentManager: FragmentManager,
    onNavigateToHome: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState(CourseVideosUIState.Loading)
    val uiMessage by viewModel.uiMessage.collectAsState(null)
    val context = LocalContext.current

    CourseVideosUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = uiMessage,
        onNavigateToHome = onNavigateToHome,
        onVideoClick = { videoBlock ->
            viewModel.courseRouter.navigateToCourseContainer(
                fragmentManager,
                courseId = viewModel.courseId,
                unitId = viewModel.getBlockParent(videoBlock.id)?.id ?: return@CourseVideosUI,
                mode = CourseViewMode.VIDEOS
            )
            viewModel.logVideoClick(videoBlock.id)
        },
        onDownloadClick = { blocksIds ->
            viewModel.downloadBlocks(
                blocksIds = blocksIds,
                fragmentManager = fragmentManager,
                context = context,
            )
        },
        onCompletedSectionVisibilityChange = {
            viewModel.onCompletedSectionVisibilityChange()
        },
    )
}

@Composable
private fun CourseVideosUI(
    windowSize: WindowSize,
    uiState: CourseVideosUIState,
    uiMessage: UIMessage?,
    onNavigateToHome: () -> Unit,
    onVideoClick: (Block) -> Unit,
    onDownloadClick: (blocksIds: List<String>) -> Unit,
    onCompletedSectionVisibilityChange: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.appColors.background
    ) {
        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val listBottomPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(bottom = 24.dp),
                    compact = PaddingValues(bottom = 24.dp)
                )
            )
        }

       /* HandleUIMessage(
            uiMessage = uiMessage, snackbarHostState = snackbarHostState,
            scaffoldState = TODO(),
            onDisplayed = TODO()
        )*/

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = screenWidth,
                color = MaterialTheme.appColors.background
            ) {
                Box {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (uiState) {
                            is CourseVideosUIState.Empty -> {
                                CourseContentVideoEmptyState(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    onReturnToCourseClick = onNavigateToHome
                                )
                            }

                            is CourseVideosUIState.CourseData -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = listBottomPadding
                                ) {
                                    val allVideos = uiState.courseVideos.values.flatten()
                                    val hasCompletedSection =
                                        uiState.courseVideos.values.any { sectionVideos ->
                                            sectionVideos.all { video ->
                                                video.isCompleted()
                                            }
                                        }
                                    val progress = Progress(
                                        assignmentsCompleted = allVideos.filter { it.isCompleted() }.size,
                                        totalAssignmentsCount = allVideos.size,
                                    )
                                    item {
                                        CourseProgress(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    bottom = 8.dp,
                                                    start = 24.dp,
                                                    end = 24.dp,
                                                ),
                                            progress = progress,
                                            isCompletedShown = uiState.isCompletedSectionsShown,
                                            onVisibilityChanged = if (hasCompletedSection) {
                                                { onCompletedSectionVisibilityChange() }
                                            } else {
                                                null
                                            },
                                            description = stringResource(
                                                coreR.string.course_completed_of,
                                                progress.assignmentsCompleted,
                                                progress.totalAssignmentsCount
                                            )
                                        )
                                    }
                                    item {
                                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                                    }

                                    uiState.courseStructure.blockData
                                        .let { list ->
                                            if (true) {
                                                list.sortedBy { section ->
                                                    uiState.courseVideos[section.id]?.any { !it.isCompleted() }
                                                }
                                            } else {
                                                list
                                            }
                                        }
                                        .forEach { section ->
                                            val sectionVideos =
                                                uiState.courseVideos[section.id] ?: emptyList()

                                            val shouldShowSection =
                                                sectionVideos.any { !it.isCompleted() } ||
                                                        uiState.isCompletedSectionsShown
                                            if (shouldShowSection) {
                                                item {
                                                    CourseVideoSection(
                                                        block = section,
                                                        videoBlocks = sectionVideos,
                                                        downloadedStateMap = uiState.downloadedState,
                                                        onVideoClick = onVideoClick,
                                                        onDownloadClick = onDownloadClick,
                                                        preview = uiState.videoPreview,
                                                        progress = uiState.videoProgress,
                                                    )
                                                }
                                            }
                                        }
                                }
                            }

                            CourseVideosUIState.Loading -> {
                                CircularProgress()
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CourseVideoSection(
    block: Block,
    videoBlocks: List<Block>,
    preview: Map<String, VideoPreview?>,
    progress: Map<String, Float?>,
    downloadedStateMap: Map<String, DownloadedState>,
    onVideoClick: (Block) -> Unit,
    onDownloadClick: (blocksIds: List<String>) -> Unit,
) {
    val state = rememberLazyListState()
    val subSectionIds = videoBlocks.map { it.id }
    val filteredStatuses = downloadedStateMap.filterKeys { it in subSectionIds }.values
    val downloadedState = when {
        filteredStatuses.isEmpty() -> null
        filteredStatuses.all { it.isDownloaded } -> DownloadedState.DOWNLOADED
        filteredStatuses.any { it.isWaitingOrDownloading } -> DownloadedState.DOWNLOADING
        else -> DownloadedState.NOT_DOWNLOADED
    }
    val videoCardWidth = 192.dp
    val rowHorizontalArrangement = 8.dp

    LaunchedEffect(Unit) {
        try {
            val uncompletedBlockIndex = videoBlocks.indexOf(videoBlocks.find { !it.isCompleted() })
            state.scrollToItem(uncompletedBlockIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    val downloadBlockIds = downloadedStateMap.keys.filter { it in block.descendants }
    var showDeleteVideoDialog by remember { mutableStateOf(false) }

    if (showDeleteVideoDialog) {
        ShowDeleteVideoConfirmationDialog(
            blockTitle = block.displayName,
            onDismissClick = {
                showDeleteVideoDialog = false
            },
            onDownloadClick = {
                showDeleteVideoDialog = false
                onDownloadClick(downloadBlockIds)
            },
        )
    }
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        CourseVideoSectionHeader(
            block = block,
            downloadedState = downloadedState,
            videoBlocks = videoBlocks,
            onDownloadClick = {
                if (downloadedState == DownloadedState.DOWNLOADED) {
                    showDeleteVideoDialog = true
                } else {
                    showDeleteVideoDialog = false
                    onDownloadClick(downloadBlockIds)
                }
            }
        )
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(rowHorizontalArrangement),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = videoCardWidth + rowHorizontalArrangement,
            )
        ) {
            items(videoBlocks) { block ->
                val localProgress = progress[block.id]
                val progress = localProgress ?: if (block.isCompleted()) {
                    1f
                } else {
                    0f
                }
                CourseVideoItem(
                    modifier = Modifier
                        .width(videoCardWidth)
                        .height(108.dp)
                        .clip(MaterialTheme.appShapes.videoPreviewShape),
                    videoBlock = block,
                    preview = preview[block.id],
                    progress = progress,
                    onClick = {
                        onVideoClick(block)
                    }
                )
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}
@Composable
fun CourseVideoSectionHeader(
    modifier: Modifier = Modifier,
    block: Block,
    videoBlocks: List<Block>?,
    downloadedState: DownloadedState?,
    onDownloadClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = block.displayName,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.course_video_watched,
                    videoBlocks?.filter { it.isCompleted() }?.size ?: 0,
                    videoBlocks?.size ?: 0
                ),
                style = MaterialTheme.appTypography.bodySmall,
                color = MaterialTheme.appColors.textPrimary,
            )
        }
        DownloadIcon(
            downloadedState = downloadedState,
            onDownloadClick = onDownloadClick
        )
    }
}
@Composable
fun DownloadIcon(
    downloadedState: DownloadedState?,
    onDownloadClick: () -> Unit,
) {
    val iconModifier = Modifier.size(24.dp)
    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (downloadedState == DownloadedState.DOWNLOADED || downloadedState == DownloadedState.NOT_DOWNLOADED) {
            val downloadIcon = if (downloadedState == DownloadedState.DOWNLOADED) {
                Icons.Default.CloudDone
            } else {
                Icons.Outlined.CloudDownload
            }
            val downloadIconDescription = if (downloadedState == DownloadedState.DOWNLOADED) {
                stringResource(id = R.string.course_accessibility_remove_course_section)
            } else {
                stringResource(id = R.string.course_accessibility_download_course_section)
            }
            val downloadIconTint = if (downloadedState == DownloadedState.DOWNLOADED) {
                MaterialTheme.appColors.successGreen
            } else {
                MaterialTheme.appColors.primary
            }
            IconButton(
                modifier = iconModifier,
                onClick = { onDownloadClick() }
            ) {
                Icon(
                    imageVector = downloadIcon,
                    contentDescription = downloadIconDescription,
                    tint = downloadIconTint
                )
            }
        } else if (downloadedState != null) {
            Box(contentAlignment = Alignment.Center) {
                if (downloadedState == DownloadedState.DOWNLOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        trackColor = Color.LightGray,
                        strokeWidth = 2.dp,
                        color = MaterialTheme.appColors.primary
                    )
                } else if (downloadedState == DownloadedState.WAITING) {
                    Icon(
                        painter = painterResource(id = coreR.drawable.course_download_waiting),
                        contentDescription = stringResource(
                            id = R.string.course_accessibility_stop_downloading_course_section
                        ),
                        tint = MaterialTheme.appColors.error
                    )
                }
                IconButton(
                    modifier = iconModifier.padding(2.dp),
                    onClick = { onDownloadClick() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(
                            id = R.string.course_accessibility_stop_downloading_course_section
                        ),
                        tint = MaterialTheme.appColors.error
                    )
                }
            }
        }
    }
}

/*@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseVideosScreenPreview() {
    OpenEdXTheme {
        CourseVideosUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiMessage = null,
            uiState = CourseVideosUIState.CourseData(
                CoreMocks.mockCourseStructure,
                emptyMap(),
                mapOf(),
                mapOf(),
                CoreMocks.mockDownloadModelsSize,
                isCompletedSectionsShown = false,
                videoPreview = mapOf(),
                videoProgress = mapOf(),
            ),
            onVideoClick = { },
            onDownloadClick = {},
            onCompletedSectionVisibilityChange = {},
            onNavigateToHome = {},
        )
    }
}*/

/*@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseVideosScreenEmptyPreview() {
    OpenEdXTheme {
        CourseVideosUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiMessage = null,
            uiState = CourseVideosUIState.Empty,
            onVideoClick = { },
            onDownloadClick = {},
            onCompletedSectionVisibilityChange = {},
            onNavigateToHome = {},
        )
    }
}*/
/*
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseVideosScreenTabletPreview() {
    OpenEdXTheme {
        CourseVideosUI(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiMessage = null,
            uiState = CourseVideosUIState.CourseData(
                CoreMocks.mockCourseStructure,
                emptyMap(),
                mapOf(),
                mapOf(),
                CoreMocks.mockDownloadModelsSize.copy(
                    allCount = 0
                ),
                isCompletedSectionsShown = true,
                videoPreview = mapOf(),
                videoProgress = mapOf(),
            ),
            onVideoClick = { },
            onDownloadClick = {},
            onCompletedSectionVisibilityChange = {},
            onNavigateToHome = {},
        )
    }
}*/
