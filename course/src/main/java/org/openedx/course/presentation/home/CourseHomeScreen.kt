package org.openedx.course.presentation.home

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.NoContentScreenType
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.NoContentScreen
import org.openedx.core.ui.OpenEdXBrandButton
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.R
import org.openedx.course.presentation.container.CourseContainerTab
import org.openedx.course.presentation.container.CourseContentTab
import org.openedx.course.presentation.container.CourseHomePagerTab
import org.openedx.course.presentation.ui.CourseMessage
import org.openedx.course.presentation.worker.CoreMocks
import org.openedx.core.R as coreR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseHomeScreen(
    windowSize: WindowSize,
    viewModel: CourseHomeViewModel,
    fragmentManager: FragmentManager,
    homePagerState: PagerState,
    onNavigateToContent: (CourseContentTab) -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    //val uiMessage by viewModel.Ui.collectAsState(null)
    val resumeBlockId by viewModel.resumeBlockId.collectAsState("")
    val context = LocalContext.current

    LaunchedEffect(resumeBlockId) {
        if (resumeBlockId.isNotEmpty()) {
            viewModel.openBlock(fragmentManager, resumeBlockId)
        }
    }

    CourseHomeUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = null,
        homePagerState = homePagerState,
        onSubSectionClick = { subSectionBlock ->
            // Log section/subsection click event
            viewModel.logSectionSubsectionClick(
                subSectionBlock.blockId,
                subSectionBlock.displayName
            )
            if (viewModel.isCourseDropdownNavigationEnabled) {
                viewModel.courseSubSectionUnit[subSectionBlock.id]?.let { unit ->
                    viewModel.courseRouter.navigateToCourseContainer(
                        fragmentManager,
                        courseId = viewModel.courseId,
                        unitId = unit.id,
                        mode = CourseViewMode.FULL
                    )
                }
            } else {
                viewModel.courseRouter.navigateToCourseSubsections(
                    fm = fragmentManager,
                    courseId = viewModel.courseId,
                    subSectionId = subSectionBlock.id,
                    mode = CourseViewMode.FULL
                )
            }
        },
        onResumeClick = { componentId ->
            viewModel.openBlock(
                fragmentManager,
                componentId
            )
        },
        onDownloadClick = { blocksIds ->
            viewModel.downloadBlocks(
                blocksIds = blocksIds,
                fragmentManager = fragmentManager,
            )
        },
        onCertificateClick = {
            viewModel.viewCertificateTappedEvent()
            it.takeIfNotEmpty()
                ?.let { url -> AndroidUriHandler(context).openUri(url) }
        },
        onVideoClick = { videoBlock ->
            viewModel.courseRouter.navigateToCourseContainer(
                fragmentManager,
                courseId = viewModel.courseId,
                unitId = viewModel.getBlockParent(videoBlock.id)?.id ?: return@CourseHomeUI,
                mode = CourseViewMode.VIDEOS
            )
            viewModel.logVideoClick(videoBlock.id)
        },
        onAssignmentClick = { assignmentBlock ->
            viewModel.courseRouter.navigateToCourseContainer(
                fragmentManager,
                courseId = viewModel.courseId,
                unitId = viewModel.getBlockParent(assignmentBlock.id)?.id ?: return@CourseHomeUI,
                mode = CourseViewMode.FULL
            )
            viewModel.logAssignmentClick(assignmentBlock.id)
        },
        onNavigateToContent = onNavigateToContent,
        onNavigateToProgress = onNavigateToProgress,
        getBlockParent = viewModel::getBlockParent,
        onViewAllContentClick = viewModel::logViewAllContentClick,
        onViewAllVideosClick = viewModel::logViewAllVideosClick,
        onViewAllAssignmentsClick = viewModel::logViewAllAssignmentsClick,
        onViewProgressClick = viewModel::logViewProgressClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CourseHomeUI(
    windowSize: WindowSize,
    uiState: CourseHomeUIState,
    uiMessage: UIMessage?,
    homePagerState: PagerState,
    onSubSectionClick: (Block) -> Unit,
    onResumeClick: (String) -> Unit,
    onDownloadClick: (blockIds: List<String>) -> Unit,
    onCertificateClick: (String) -> Unit,
    onVideoClick: (Block) -> Unit,
    onAssignmentClick: (Block) -> Unit,
    onNavigateToContent: (CourseContentTab) -> Unit,
    onNavigateToProgress: () -> Unit,
    getBlockParent: (blockId: String) -> Block?,
    onViewAllContentClick: () -> Unit,
    onViewAllVideosClick: () -> Unit,
    onViewAllAssignmentsClick: () -> Unit,
    onViewProgressClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.appColors.background
    ) {
        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 720.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        //HandleUIMessage(uiMessage = uiMessage, snackbarHostState = snackbarHostState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = it.calculateStartPadding(LayoutDirection.Ltr),
                    end = it.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = it.calculateBottomPadding()
                )
                .padding(top = 8.dp)
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = screenWidth,
                color = MaterialTheme.appColors.background
            ) {
                when (uiState) {
                    is CourseHomeUIState.CourseData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            val certificate = uiState.courseStructure.certificate
                            if (certificate?.isCertificateEarned() == true) {
                                CourseMessage(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 12.dp,
                                            horizontal = 24.dp
                                        ),
                                    icon = painterResource(R.drawable.ic_course_certificate),
                                    message = stringResource(
                                        R.string.course_you_earned_certificate,
                                        uiState.courseStructure.name
                                    ),
                                    action = stringResource(R.string.course_view_certificate),
                                    onActionClick = {
                                        onCertificateClick(
                                            certificate.certificateURL ?: ""
                                        )
                                    }
                                )
                            }

                            if (uiState.resumeComponent != null) {
                                ResumeCourseButton(
                                    modifier = Modifier.padding(
                                        start = 14.dp,
                                        end = 14.dp,
                                        bottom = 8.dp,
                                        top = 10.dp
                                    ),
                                    block = uiState.resumeComponent,
                                    displayName = uiState.resumeUnitTitle,
                                    onResumeClick = onResumeClick
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            CourseHomePager(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(450.dp), // <- ensures Compose lays it out
                                pages = CourseHomePagerTab.entries,
                                pagerState = homePagerState
                            ) { page ->
                                Log.d("PagerDebug", "Composing page $page")
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.appColors.cardViewBackground
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.appColors.cardViewBorder
                                    ),
                                    shape = MaterialTheme.appShapes.cardShape,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    when (page) {
                                        CourseHomePagerTab.COURSE_COMPLETION -> {

                                            CourseCompletionHomePagerCardContent(
                                                uiState = uiState,
                                                onViewAllContentClick = {
                                                    onViewAllContentClick()
                                                    onNavigateToContent(CourseContentTab.ALL)
                                                },
                                                onDownloadClick = onDownloadClick,
                                                onSubSectionClick = onSubSectionClick
                                            )
                                        }

                                        CourseHomePagerTab.VIDEOS -> {
                                            VideosHomePagerCardContent(
                                                uiState = uiState,
                                                onVideoClick = onVideoClick,
                                                onViewAllVideosClick = {
                                                    onViewAllVideosClick()
                                                    onNavigateToContent(CourseContentTab.VIDEOS)
                                                }
                                            )
                                        }

                                        CourseHomePagerTab.ASSIGNMENT -> {
                                            AssignmentsHomePagerCardContent(
                                                uiState = uiState,
                                                onAssignmentClick = onAssignmentClick,
                                                getBlockParent = getBlockParent,
                                                onViewAllAssignmentsClick = {
                                                    onViewAllAssignmentsClick()
                                                    onNavigateToContent(CourseContentTab.ASSIGNMENTS)
                                                }
                                            )
                                        }

                                        CourseHomePagerTab.GRADES -> {
                                            GradesHomePagerCardContent(
                                                uiState = uiState,
                                                onViewProgressClick = {
                                                    onViewProgressClick()
                                                    onNavigateToProgress()
                                                }
                                            )
                                        }
                                    }

                                }
                            }
                        }
                    }

                    CourseHomeUIState.Error -> {
                        NoContentScreen(noContentScreenType = NoContentScreenType.COURSE_OUTLINE)
                    }

                    CourseHomeUIState.Loading -> {
                        CircularProgress()
                    }

                    CourseHomeUIState.Waiting -> {}
                }
            }
        }
    }
}
@Composable
fun ResumeCourseButton(
    modifier: Modifier = Modifier,
    block: Block,
    displayName: String,
    onResumeClick: (String) -> Unit,
) {
    OpenEdXBrandButton(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 45.dp),
        onClick = {
            onResumeClick(block.id)
        },
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = displayName,
                    color = MaterialTheme.appColors.primaryButtonText,
                    style = MaterialTheme.appTypography.titleSmall,
                    fontWeight = FontWeight.W500
                )
                TextIcon(
                    text = stringResource(id = R.string.course_continue),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    color = MaterialTheme.appColors.primaryButtonText,
                    textStyle = MaterialTheme.appTypography.labelMedium
                )
            }
        }
    )
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> CourseHomePager(
    modifier: Modifier = Modifier,
    pages: List<T>,
    pagerState: PagerState,
    pageContent: @Composable (T) -> Unit
) {
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 8.dp,
        beyondViewportPageCount = pages.size,
        verticalAlignment = Alignment.Top
    ) { page ->
        pageContent(pages[page])
    }
}

@Composable
fun ViewAllButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            tint = MaterialTheme.appColors.textAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textAccent
        )
    }
}

@Composable
fun CaughtUpMessage(
    modifier: Modifier = Modifier,
    message: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(coreR.drawable.core_ic_check),
            contentDescription = null,
            tint = MaterialTheme.appColors.successGreen
        )
        Text(
            modifier = modifier
                .fillMaxWidth(),
            text = message,
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseHomeScreenPreview() {
    OpenEdXTheme {
        val previewPagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { CourseContainerTab.entries.size }
        )
        CourseHomeUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseHomeUIState.CourseData(
                courseStructure = CoreMocks.mockCourseStructure,
                courseProgress = null, // No course progress for preview
                next = null, // No next section for preview
                downloadedState = mapOf(),
                resumeComponent = CoreMocks.mockChapterBlock,
                resumeUnitTitle = "Resumed Unit",
                courseSubSections = mapOf(),
                subSectionsDownloadsCount = mapOf(),
                useRelativeDates = true,
                courseVideos = mapOf(),
                courseAssignments = emptyList(),
                videoPreview = null,
                videoProgress = 0f
            ),
            uiMessage = null,
            homePagerState = previewPagerState,
            onSubSectionClick = {},
            onResumeClick = {},
            onDownloadClick = {},
            onCertificateClick = {},
            onVideoClick = {},
            onAssignmentClick = {},
            onNavigateToContent = { _ -> },
            onNavigateToProgress = {},
            getBlockParent = { null },
            onViewAllContentClick = {},
            onViewAllVideosClick = {},
            onViewAllAssignmentsClick = {},
            onViewProgressClick = {},
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseHomeScreenTabletPreview() {
    OpenEdXTheme {
        val previewPagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { CourseContainerTab.entries.size }
        )
        CourseHomeUI(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseHomeUIState.CourseData(
                courseStructure = CoreMocks.mockCourseStructure,
                courseProgress = null, // No course progress for preview
                next = null, // No next section for preview
                downloadedState = mapOf(),
                resumeComponent = CoreMocks.mockChapterBlock,
                resumeUnitTitle = "Resumed Unit",
                courseSubSections = mapOf(),
                subSectionsDownloadsCount = mapOf(),
                useRelativeDates = true,
                courseVideos = mapOf(),
                courseAssignments = emptyList(),
                videoPreview = null,
                videoProgress = 0f
            ),
            uiMessage = null,
            homePagerState = previewPagerState,
            onSubSectionClick = {},
            onResumeClick = {},
            onDownloadClick = {},
            onCertificateClick = {},
            onVideoClick = {},
            onAssignmentClick = {},
            onNavigateToContent = { _ -> },
            onNavigateToProgress = { },
            getBlockParent = { null },
            onViewAllContentClick = {},
            onViewAllVideosClick = {},
            onViewAllAssignmentsClick = {},
            onViewProgressClick = {},
        )
    }
}
