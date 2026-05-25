package org.openedx.course.presentation.outline

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.BlockType
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.Progress
import org.openedx.core.extension.getChapterBlocks
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.R
import org.openedx.course.presentation.container.CourseContentAllEmptyState
import org.openedx.course.presentation.home.ResumeCourseButton
import org.openedx.course.presentation.ui.CardArrow
import org.openedx.course.presentation.ui.CourseMessage
import org.openedx.course.presentation.ui.CourseSection
import org.openedx.course.presentation.worker.CoreMocks

@Composable
fun CourseContentAllScreen(
    windowSize: WindowSize,
    viewModel: CourseContentAllViewModel,
    fragmentManager: FragmentManager,
    onNavigateToHome: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState(null)
    val resumeBlockId by viewModel.resumeBlockId.collectAsState("")
    val context = LocalContext.current

    LaunchedEffect(resumeBlockId) {
        if (resumeBlockId.isNotEmpty()) {
            viewModel.openBlock(fragmentManager, resumeBlockId)
        }
    }

    CourseContentAllUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = uiMessage,
        onNavigateToHome = onNavigateToHome,
        onExpandClick = { block ->
            if (viewModel.switchCourseSections(block.id)) {
                viewModel.sequentialClickedEvent(
                    block.blockId,
                    block.displayName
                )
            }
        },
        onSubSectionClick = { subSectionBlock ->
            if (viewModel.isCourseDropdownNavigationEnabled) {
                viewModel.courseSubSectionUnit[subSectionBlock.id]?.let { unit ->
                    viewModel.logUnitDetailViewedEvent(
                        unit.blockId,
                        unit.displayName
                    )
                    viewModel.courseRouter.navigateToCourseContainer(
                        fragmentManager,
                        courseId = viewModel.courseId,
                        unitId = unit.id,
                        mode = CourseViewMode.FULL
                    )
                }
            } else {
                viewModel.sequentialClickedEvent(
                    subSectionBlock.blockId,
                    subSectionBlock.displayName
                )
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
        }
    )
}

@Composable
private fun CourseContentAllUI(
    windowSize: WindowSize,
    uiState: CourseContentAllUIState,
    uiMessage: UIMessage?,
    onNavigateToHome: () -> Unit,
    onExpandClick: (Block) -> Unit,
    onSubSectionClick: (Block) -> Unit,
    onResumeClick: (String) -> Unit,
    onDownloadClick: (blockIds: List<String>) -> Unit,
    onCertificateClick: (String) -> Unit,
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

        val listPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.padding(horizontal = 6.dp),
                    compact = Modifier.padding(horizontal = 24.dp)
                )
            )
        }

      /*  HandleUIMessage(
            uiMessage = uiMessage,
            scaffoldState = snackbarHostState,
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
                    when (uiState) {
                        is CourseContentAllUIState.CourseData -> {
                            if (uiState.courseStructure.blockData.isEmpty()) {
                                CourseContentAllEmptyState(
                                    onReturnToCourseClick = onNavigateToHome
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = listBottomPadding
                                ) {
                                    val certificate = uiState.courseStructure.certificate
                                    if (certificate?.isCertificateEarned() == true) {
                                        item {
                                            CourseMessage(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp)
                                                    .then(listPadding),
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
                                    }

                                    val sections =
                                        uiState.courseStructure.blockData.getChapterBlocks()
                                    val progress = Progress(
                                        totalAssignmentsCount = sections.size,
                                        assignmentsCompleted = sections.filter { it.isCompleted() }.size
                                    )
                                    item {
                                        CourseProgress(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 24.dp,
                                                    end = 24.dp
                                                ),
                                            progress = progress,
                                            description = pluralStringResource(
                                                R.plurals.course_sections_complete,
                                                progress.assignmentsCompleted,
                                                progress.assignmentsCompleted,
                                                progress.totalAssignmentsCount
                                            )
                                        )
                                    }

                                    if (uiState.resumeComponent != null) {
                                        item {
                                            Box(listPadding) {
                                                ResumeCourseButton(
                                                    modifier = Modifier.padding(vertical = 16.dp),
                                                    block = uiState.resumeComponent,
                                                    displayName = uiState.resumeUnitTitle,
                                                    onResumeClick = onResumeClick
                                                )
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    uiState.courseStructure.blockData.forEach { section ->
                                        val courseSubSections =
                                            uiState.courseSubSections[section.id]
                                        val courseSectionsState =
                                            uiState.courseSectionsState[section.id]

                                        item {
                                            CourseSection(
                                                modifier = listPadding.padding(vertical = 4.dp),
                                                onItemClick = onExpandClick,
                                                downloadedStateMap = uiState.downloadedState,
                                                onSubSectionClick = onSubSectionClick,
                                                onDownloadClick = onDownloadClick,
                                                block = section,
                                                courseSectionsState = courseSectionsState,
                                                courseSubSections = courseSubSections
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CourseContentAllUIState.Error -> {
                            CourseContentAllEmptyState(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                onReturnToCourseClick = onNavigateToHome
                            )
                        }

                        CourseContentAllUIState.Loading -> {
                            CircularProgress()
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CourseProgress(
    modifier: Modifier = Modifier,
    progress: Progress,
    description: String,
    isCompletedShown: Boolean = false,
    onVisibilityChanged: (() -> Unit)? = null
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isCompletedShown) {
            -90f
        } else {
            90f
        },
        label = ""
    )
    val buttonText = if (isCompletedShown) {
        stringResource(R.string.course_hide_completed)
    } else {
        stringResource(R.string.course_view_completed)
    }
    Column(
        modifier = modifier,
    ) {
        androidx.compose.material3.LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            progress = {  progress.value  },
            color = MaterialTheme.appColors.progressBarColor,
            trackColor = MaterialTheme.appColors.progressBarBackgroundColor,
            strokeCap = StrokeCap.Square
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = description,
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.labelSmall
            )
            if (onVisibilityChanged != null) {
                Row(
                    modifier = Modifier.clickable {
                        onVisibilityChanged()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = buttonText,
                        color = MaterialTheme.appColors.textAccent,
                        style = MaterialTheme.appTypography.labelMedium
                    )
                    CardArrow(
                        degrees = arrowRotation
                    )
                }
            }
        }
    }
}

@Composable
fun LinearProgressIndicator(
    modifier: Modifier,
    progress: () -> Float,
    color: Color,
    trackColor: Color,
    strokeCap: StrokeCap,
    gapSize: Dp,
    drawStopIndicator: () -> Unit
) {
    TODO("Not yet implemented")
}


@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseOutlineScreenPreview() {
    OpenEdXTheme {
        CourseContentAllUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseContentAllUIState.CourseData(
                CoreMocks.mockCourseStructure,
                mapOf(),
                CoreMocks.mockChapterBlock,
                "Resumed Unit",
                mapOf(),
                mapOf(),
                mapOf(),
                true
            ),
            uiMessage = null,
            onExpandClick = {},
            onSubSectionClick = {},
            onResumeClick = {},
            onDownloadClick = {},
            onCertificateClick = {},
            onNavigateToHome = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseContentAllScreenTabletPreview() {
    OpenEdXTheme {
        CourseContentAllUI(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseContentAllUIState.CourseData(
                CoreMocks.mockCourseStructure,
                mapOf(),
                CoreMocks.mockChapterBlock,
                "Resumed Unit",
                mapOf(),
                mapOf(),
                mapOf(),
                true
            ),
            uiMessage = null,
            onExpandClick = {},
            onSubSectionClick = {},
            onResumeClick = {},
            onDownloadClick = {},
            onCertificateClick = {},
            onNavigateToHome = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ResumeCoursePreview() {
    OpenEdXTheme {
        ResumeCourseButton(block = CoreMocks.mockChapterBlock, displayName = "Resumed Unit") {}
    }
}
