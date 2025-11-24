package org.openedx.discussion.presentation.ui

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.extension.TextConverter
import org.openedx.core.ui.AutoSizeText
import org.openedx.core.ui.HyperlinkImageText
import org.openedx.core.ui.IconText
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.discussion.R
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.domain.model.Thread
import org.openedx.discussion.domain.model.Topic
import org.openedx.discussion.presentation.comments.DiscussionCommentsFragment
import org.openedx.core.R as CoreR

@Composable
fun ThreadMainItem(
    modifier: Modifier,
    thread: Thread,
    onClick: (String, Boolean) -> Unit,
    onUserPhotoClick: (String) -> Unit
) {
    val profileImageUrl = if (thread.users?.get(thread.author)?.image?.hasImage == true) {
        thread.users[thread.author]?.image?.imageUrlFull
    } else {
        CoreR.drawable.core_ic_default_profile_picture
    }

    val followText = if (thread.following) {
        stringResource(id = R.string.discussion_following)
    } else {
        stringResource(id = R.string.discussion_follow)
    }
    val followIcon = if (thread.following) {
        R.drawable.discussion_star_filled
    } else {
        R.drawable.discussion_star
    }

    val voteIcon = if (thread.voted) {
        Icons.Filled.ThumbUp
    } else {
        Icons.Outlined.ThumbUp
    }
    val reportText = if (thread.abuseFlagged) {
        stringResource(id = R.string.discussion_unreport)
    } else {
        stringResource(id = R.string.discussion_report)
    }
    val reportColor = if (thread.abuseFlagged) {
        MaterialTheme.appColors.error
    } else {
        MaterialTheme.appColors.textPrimary
    }

    val context = LocalContext.current

    Column(
        modifier = modifier.background(MaterialTheme.appColors.background)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImageUrl)
                    .error(CoreR.drawable.core_ic_default_profile_picture)
                    .placeholder(CoreR.drawable.core_ic_default_profile_picture)
                    .build(),
                contentDescription = stringResource(
                    id = CoreR.string.core_accessibility_user_profile_image,
                    thread.author
                ),
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.appShapes.material.medium)
                    .clickable {
                        if (thread.author.isNotEmpty()) {
                            onUserPhotoClick(thread.author)
                        }
                    }
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    modifier = Modifier
                        .clickable {
                            if (thread.author.isNotEmpty()) {
                                onUserPhotoClick(thread.author)
                            }
                        },
                    text = thread.author.ifEmpty { stringResource(id = R.string.discussion_anonymous) },
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium
                )
                Text(
                    text = TimeUtils.iso8601ToDateWithTime(context, thread.createdAt),
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textPrimaryVariant
                )
            }
            IconText(
                text = followText,
                painter = painterResource(followIcon),
                textStyle = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.textPrimary,
                onClick = {
                    onClick(DiscussionCommentsFragment.ACTION_FOLLOW_THREAD, !thread.following)
                })
        }
        Spacer(modifier = Modifier.height(24.dp))
        HyperlinkImageText(
            title = thread.title,
            imageText = thread.parsedRenderedBody,
            linkTextColor = MaterialTheme.appColors.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconText(
                text = pluralStringResource(
                    id = R.plurals.discussion_votes,
                    thread.voteCount,
                    thread.voteCount
                ),
                modifier = Modifier.alpha(if (thread.isAuthor) 0.3f else 1f),
                icon = voteIcon,
                color = MaterialTheme.appColors.textPrimary,
                textStyle = MaterialTheme.appTypography.labelLarge,
                onClick = {
                    if (thread.isAuthor.not()) {
                        onClick(DiscussionCommentsFragment.ACTION_UPVOTE_THREAD, !thread.voted)
                    }
                }
            )
            IconText(
                text = reportText,
                painter = painterResource(id = R.drawable.discussion_ic_report),
                textStyle = MaterialTheme.appTypography.labelLarge,
                color = reportColor,
                onClick = {
                    onClick(DiscussionCommentsFragment.ACTION_REPORT_THREAD, !thread.abuseFlagged)
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.appColors.cardViewBorder)
    }

}

@Composable
fun CommentItem(
    modifier: Modifier,
    comment: DiscussionComment,
    shape: Shape = MaterialTheme.appShapes.cardShape,
    onCommentPulseEnd: (DiscussionComment) -> Unit = {},
    onClick: (String, String, Boolean) -> Unit,
    onAddCommentClick: () -> Unit = {},
    onUserPhotoClick: (String) -> Unit,
) {
    val profileImageUrl = if (comment.profileImage?.hasImage == true) {
        comment.profileImage.imageUrlFull
    } else if (comment.users?.get(comment.author)?.image?.hasImage == true) {
        comment.users[comment.author]?.image?.imageUrlFull
    } else {
        CoreR.drawable.core_ic_default_profile_picture
    }

    val reportText = if (comment.abuseFlagged) {
        stringResource(id = R.string.discussion_unreport)
    } else {
        stringResource(id = R.string.discussion_report)
    }

    val reportColor = if (comment.abuseFlagged) {
        MaterialTheme.appColors.error
    } else {
        MaterialTheme.appColors.textPrimary
    }
    val voteIcon = if (comment.voted) {
        Icons.Filled.ThumbUp
    } else {
        Icons.Outlined.ThumbUp
    }

    val highlightColor = MaterialTheme.appColors.highlightDiscussionResponse
    val normalColor = MaterialTheme.appColors.cardViewBackground

    val backgroundColor = remember { Animatable(normalColor) }

    LaunchedEffect(comment.shouldHighlight) {
        if (comment.shouldHighlight) {
            repeat(5) {
                backgroundColor.animateTo(highlightColor, animationSpec = tween(250))
                backgroundColor.animateTo(normalColor, animationSpec = tween(250))
            }
            onCommentPulseEnd(comment)
        } else {
            backgroundColor.snapTo(normalColor)
        }
    }

    val context = LocalContext.current

    Card(
        shape = shape,
        modifier = modifier.then(
            Modifier.border(
                1.dp,
                MaterialTheme.appColors.cardViewBorder,
                shape
            )
        ),
        backgroundColor = backgroundColor.value,
        elevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImageUrl)
                        .error(CoreR.drawable.core_ic_default_profile_picture)
                        .placeholder(CoreR.drawable.core_ic_default_profile_picture)
                        .build(),
                    contentDescription = stringResource(
                        id = CoreR.string.core_accessibility_user_profile_image,
                        comment.author
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            onUserPhotoClick(comment.author)
                        }
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .clickable {
                                onUserPhotoClick(comment.author)
                            },
                        text = comment.author,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleSmall
                    )
                    Text(
                        text = TimeUtils.iso8601ToDateWithTime(context, comment.createdAt),
                        style = MaterialTheme.appTypography.labelSmall,
                        color = MaterialTheme.appColors.textPrimaryVariant
                    )
                }
                IconText(
                    text = reportText,
                    painter = painterResource(id = R.drawable.discussion_ic_report),
                    textStyle = MaterialTheme.appTypography.labelMedium,
                    color = reportColor,
                    onClick = {
                        onClick(
                            DiscussionCommentsFragment.ACTION_REPORT_COMMENT,
                            comment.id,
                            !comment.abuseFlagged
                        )
                    })
            }
            Spacer(modifier = Modifier.height(14.dp))
            HyperlinkImageText(
                imageText = comment.parsedRenderedBody,
                linkTextColor = MaterialTheme.appColors.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconText(
                    text = pluralStringResource(
                        id = R.plurals.discussion_votes,
                        comment.voteCount,
                        comment.voteCount
                    ),
                    icon = voteIcon,
                    modifier = Modifier.alpha(if (comment.isAuthor) 0.3f else 1f),
                    color = MaterialTheme.appColors.textPrimary,
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    onClick = {
                        if (comment.isAuthor.not()) {
                            onClick(
                                DiscussionCommentsFragment.ACTION_UPVOTE_COMMENT,
                                comment.id,
                                !comment.voted
                            )
                        }
                    }
                )
                IconText(
                    text = pluralStringResource(
                        id = R.plurals.discussion_comments,
                        comment.childCount,
                        comment.childCount
                    ),
                    painter = painterResource(id = R.drawable.discussion_ic_comment),
                    color = MaterialTheme.appColors.textPrimary,
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    onClick = {
                        onAddCommentClick()
                    }
                )
            }

        }
    }
}


@Composable
fun CommentMainItem(
    modifier: Modifier,
    internalPadding: Dp = 16.dp,
    comment: DiscussionComment,
    onClick: (String, String, Boolean) -> Unit,
    onUserPhotoClick: (String) -> Unit
) {
    val profileImageUrl = if (comment.profileImage?.hasImage == true) {
        comment.profileImage.imageUrlFull
    } else if (comment.users?.get(comment.author)?.image?.hasImage == true) {
        comment.users[comment.author]?.image?.imageUrlFull
    } else {
        CoreR.drawable.core_ic_default_profile_picture
    }

    val reportText = if (comment.abuseFlagged) {
        stringResource(id = R.string.discussion_unreport)
    } else {
        stringResource(id = R.string.discussion_report)
    }
    val reportColor = if (comment.abuseFlagged) {
        MaterialTheme.appColors.error
    } else {
        MaterialTheme.appColors.textPrimary
    }

    val voteIcon = if (comment.voted) {
        Icons.Filled.ThumbUp
    } else {
        Icons.Outlined.ThumbUp
    }

    val context = LocalContext.current

    Surface(
        modifier = modifier,
        color = MaterialTheme.appColors.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(internalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImageUrl)
                        .error(CoreR.drawable.core_ic_default_profile_picture)
                        .placeholder(CoreR.drawable.core_ic_default_profile_picture)
                        .build(),
                    contentDescription = stringResource(
                        id = CoreR.string.core_accessibility_user_profile_image,
                        comment.author
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            onUserPhotoClick(comment.author)
                        }
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .clickable {
                                onUserPhotoClick(comment.author)
                            },
                        text = comment.author,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    Text(
                        text = TimeUtils.iso8601ToDateWithTime(context, comment.createdAt),
                        style = MaterialTheme.appTypography.labelSmall,
                        color = MaterialTheme.appColors.textPrimaryVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HyperlinkImageText(
                imageText = comment.parsedRenderedBody,
                linkTextColor = MaterialTheme.appColors.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconText(
                    text = pluralStringResource(
                        id = R.plurals.discussion_votes,
                        comment.voteCount,
                        comment.voteCount
                    ),
                    icon = voteIcon,
                    modifier = Modifier.alpha(if (comment.isAuthor) 0.3f else 1f),
                    color = MaterialTheme.appColors.textPrimary,
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    onClick = {
                        if (comment.isAuthor.not()) {
                            onClick(
                                DiscussionCommentsFragment.ACTION_UPVOTE_COMMENT,
                                comment.id,
                                !comment.voted
                            )
                        }
                    }
                )
                IconText(
                    text = reportText,
                    painter = painterResource(id = R.drawable.discussion_ic_report),
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    color = reportColor,
                    onClick = {
                        onClick(
                            DiscussionCommentsFragment.ACTION_REPORT_COMMENT,
                            comment.id,
                            !comment.abuseFlagged
                        )
                    })
            }

        }
    }
}

@Composable
fun ThreadItem(
    thread: Thread,
    onClick: (Thread) -> Unit,
    onBackClick: () -> Unit,
) {
    val icon = when (thread.type) {
        DiscussionType.DISCUSSION -> painterResource(id = R.drawable.discussion_ic_discussion)
        DiscussionType.QUESTION -> rememberVectorPainter(image = Icons.AutoMirrored.Outlined.HelpOutline)
    }
    val textType = when (thread.type) {
        DiscussionType.DISCUSSION -> stringResource(id = R.string.discussion_discussion)
        DiscussionType.QUESTION -> stringResource(id = R.string.discussion_question)
    }

    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.background)
            .clickable { onClick(thread) }
            .padding(vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconText(
                text = textType,
                painter = icon,
                color = MaterialTheme.appColors.textPrimaryVariant,
                textStyle = MaterialTheme.appTypography.labelSmall
            )
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val iconSize = (MaterialTheme.appTypography.labelSmall.fontSize.value + 4).dp
                if (thread.unreadCommentCount > 0 && !thread.read) {
                    Box {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            painter = painterResource(id = R.drawable.discussion_ic_unread_replies),
                            tint = MaterialTheme.appColors.textPrimaryVariant,
                            contentDescription = null
                        )
                        Image(
                            modifier = Modifier.size(iconSize),
                            painter = painterResource(id = R.drawable.discussion_ic_unread_replies_dot),
                            contentDescription = null
                        )
                    }
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = pluralStringResource(
                            id = R.plurals.discussion_missed_posts,
                            thread.unreadCommentCount,
                            thread.unreadCommentCount
                        ),
                        color = MaterialTheme.appColors.textPrimaryVariant,
                        style = MaterialTheme.appTypography.labelSmall
                    )
                }
                if (thread.pinned) {
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.appColors.textPrimaryVariant,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = thread.title,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                id = R.string.discussion_last_post,
                TimeUtils.iso8601ToDateWithTime(context, thread.updatedAt)
            ),
            style = MaterialTheme.appTypography.labelSmall,
            color = MaterialTheme.appColors.textPrimaryVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        IconText(
            text = pluralStringResource(
                id = R.plurals.discussion_responses,
                thread.commentCount - 1,
                thread.commentCount - 1
            ),
            painter = painterResource(id = R.drawable.discussion_ic_responses),
            color = MaterialTheme.appColors.textPrimary,
            textStyle = MaterialTheme.appTypography.labelLarge
        )
    }
}


@Composable
fun ThreadItemCategory(
    name: String,
    painterResource: Painter,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.then(
            Modifier
                .border(
                    1.dp,
                    MaterialTheme.appColors.cardViewBorder,
                    MaterialTheme.appShapes.cardShape
                )
                .clip(MaterialTheme.appShapes.cardShape)
                .clickable { onClick() }),
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.cardViewBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource,
                contentDescription = null,
                tint = MaterialTheme.appColors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            AutoSizeText(
                text = name,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TopicItem(
    topic: Topic,
    onClick: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(topic.id, topic.name) }
            .padding(horizontal = 8.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = topic.name, style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            tint = MaterialTheme.appColors.primary,
            contentDescription = "Expandable Arrow"
        )
    }

}

@Preview
@Composable
private fun TopicItemPreview() {
    OpenEdXTheme {
        TopicItem(
            topic = mockTopic,
            onClick = { _, _ -> },
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ThreadItemPreview() {
    OpenEdXTheme {
        ThreadItem(
            thread = mockThread,
            onClick = {},
            onBackClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CommentItemPreview() {
    OpenEdXTheme {
        CommentItem(
            modifier = Modifier.fillMaxWidth(),
            comment = mockComment,
            onClick = { _, _, _ -> },
            onUserPhotoClick = {}
        )
    }
}

@Preview
@Composable
private fun ThreadMainItemPreview() {
    OpenEdXTheme {
        ThreadMainItem(
            modifier = Modifier.fillMaxWidth(),
            thread = mockThread,
            onClick = { _, _ -> },
            onUserPhotoClick = {}
        )
    }
}

private val mockComment = DiscussionComment(
    id = "",
    author = "ABC",
    authorLabel = "",
    createdAt = "",
    updatedAt = "",
    rawBody = "",
    renderedBody = "",
    parsedRenderedBody = TextConverter.textToLinkedImageText("mock Comment"),
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

private val mockThread = Thread(
    id = "",
    author = "ABC",
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

private val mockTopic = Topic(
    id = "",
    name = "All Topics",
    threadListUrl = "",
    children = emptyList()
)
