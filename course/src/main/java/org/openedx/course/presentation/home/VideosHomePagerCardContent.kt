package org.openedx.course.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.domain.model.Block
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.presentation.container.ContentTabEmptyState
import org.openedx.core.R as coreR
import org.openedx.core.utils.VideoPreview
import org.openedx.course.presentation.worker.CoreMocks

@Composable
fun VideosHomePagerCardContent(
    uiState: CourseHomeUIState.CourseData,
    onVideoClick: (Block) -> Unit,
    onViewAllVideosClick: () -> Unit
) {
    val allVideos = uiState.courseVideos.values.flatten()
    if (allVideos.isEmpty()) {
        CourseContentVideoEmptyState(
            onReturnToCourseClick = {},
            showReturnButton = false
        )
        return
    }

    val completedVideos = allVideos.count { it.isCompleted() }
    val totalVideos = allVideos.size
    val firstIncompleteVideo = allVideos.find { !it.isCompleted() }
    val videoProgress = uiState.videoProgress ?: if (firstIncompleteVideo?.isCompleted() ?: false) {
        1f
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with progress
        Text(
            text = stringResource(R.string.course_container_content_tab_video),
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$completedVideos/$totalVideos",
                style = MaterialTheme.appTypography.displaySmall,
                color = MaterialTheme.appColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.course_videos_completed),
                style = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.textPrimaryVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            progress = {
                if (totalVideos > 0) completedVideos.toFloat() / totalVideos else 0f
            },
            color = MaterialTheme.appColors.progressBarColor,
            trackColor = MaterialTheme.appColors.progressBarBackgroundColor,
            strokeCap = StrokeCap.Square
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Continue Watching section
        if (firstIncompleteVideo != null) {
            val title = if (videoProgress > 0) {
                stringResource(R.string.course_continue_watching)
            } else {
                stringResource(R.string.course_next_video)
            }
            Text(
                text = title,
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Video card using CourseVideoItem
            CourseVideoItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                videoBlock = firstIncompleteVideo,
                preview = uiState.videoPreview,
                progress = videoProgress,
                onClick = {
                    onVideoClick(firstIncompleteVideo)
                },
                titleStyle = MaterialTheme.appTypography.titleMedium,
                contentModifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                progressModifier = Modifier.height(8.dp),
            )
        } else {
            CaughtUpMessage(
                message = stringResource(R.string.course_videos_caught_up)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // View All Videos button
        ViewAllButton(
            text = stringResource(R.string.course_view_all_videos),
            onClick = onViewAllVideosClick
        )
    }
}
@Composable
fun CourseContentVideoEmptyState(
    modifier: Modifier = Modifier,
    onReturnToCourseClick: () -> Unit,
    showReturnButton: Boolean = true
) {
    ContentTabEmptyState(
        modifier = modifier,
        message = stringResource(id = coreR.string.core_no_videos),
        onReturnToCourseClick = onReturnToCourseClick,
        showReturnButton = showReturnButton
    )
}
@Composable
fun CourseVideoItem(
    modifier: Modifier = Modifier,
    videoBlock: Block,
    preview: VideoPreview?,
    progress: Float,
    onClick: () -> Unit,
    titleStyle: TextStyle = MaterialTheme.appTypography.bodySmall,
    contentModifier: Modifier = Modifier.padding(8.dp),
    progressModifier: Modifier = Modifier.height(4.dp),
    playButtonSize: Dp = 32.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 3.dp,
) {
    val borderColor = borderColor ?: if (videoBlock.isCompleted()) {
        MaterialTheme.appColors.successGreen
    } else {
        Color.Transparent
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.appShapes.videoPreviewShape)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = MaterialTheme.appShapes.videoPreviewShape
            )
            .clickable { onClick() }
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(preview?.link ?: preview?.bitmap)
                .error(coreR.drawable.core_no_image_course)
                .placeholder(coreR.drawable.core_no_image_course)
                .build(),
            contentDescription = stringResource(R.string.course_accessibility_video_player),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Box(
            modifier = contentModifier.fillMaxSize()
        ) {
            Image(
                modifier = Modifier
                    .size(playButtonSize)
                    .align(Alignment.Center),
                painter = painterResource(id = R.drawable.course_video_play_button),
                contentDescription = null,
            )

            // Title (top-left)
            Text(
                text = videoBlock.displayName,
                color = Color.White,
                style = titleStyle,
                modifier = Modifier
                    .align(Alignment.TopStart),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress bar (bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                if (progress > 0.0f) {
                    LinearProgressIndicator(
                        modifier = progressModifier
                            .fillMaxWidth()
                            .clip(CircleShape),
                        progress = { progress },
                        color = if (videoBlock.isCompleted() && progress > 0.95f) {
                            MaterialTheme.appColors.progressBarColor
                        } else {
                            MaterialTheme.appColors.info
                        },
                        trackColor = MaterialTheme.appColors.progressBarBackgroundColor,
                        strokeCap = StrokeCap.Square
                    )
                }
                if (videoBlock.isCompleted()) {
                    Image(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .offset(x = 1.dp),
                        painter = painterResource(id = coreR.drawable.ic_core_check),
                        contentDescription = stringResource(R.string.course_accessibility_video_watched),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .offset(x = 1.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun VideosHomePagerCardContentPreview() {
    OpenEdXTheme {
        VideosHomePagerCardContent(
            uiState = CourseHomeUIState.CourseData(
                courseStructure = CoreMocks.mockCourseStructure,
                courseProgress = null,
                next = null,
                downloadedState = mapOf(),
                resumeComponent = null,
                resumeUnitTitle = "",
                courseSubSections = mapOf(),
                subSectionsDownloadsCount = mapOf(),
                useRelativeDates = true,
                courseVideos = mapOf("section1" to listOf(CoreMocks.mockVideoBlock)),
                courseAssignments = emptyList(),
                videoPreview = null,
                videoProgress = 0.5f
            ),
            onVideoClick = {},
            onViewAllVideosClick = {}
        )
    }
}

@Preview
@Composable
private fun VideosHomePagerCardContentEmptyPreview() {
    OpenEdXTheme {
        VideosHomePagerCardContent(
            uiState = CourseHomeUIState.CourseData(
                courseStructure = CoreMocks.mockCourseStructure,
                courseProgress = null,
                next = null,
                downloadedState = mapOf(),
                resumeComponent = null,
                resumeUnitTitle = "",
                courseSubSections = mapOf(),
                subSectionsDownloadsCount = mapOf(),
                useRelativeDates = true,
                courseVideos = emptyMap(),
                courseAssignments = emptyList(),
                videoPreview = null,
                videoProgress = null
            ),
            onVideoClick = {},
            onViewAllVideosClick = {}
        )
    }
}
