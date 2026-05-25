package org.openedx.core.domain.model

import android.content.Context
import android.webkit.URLUtil
import org.openedx.core.AppDataConstants
import org.openedx.core.BlockType
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.utils.PreviewHelper
import org.openedx.core.utils.VideoPreview
import org.openedx.core.utils.VideoUtil
import java.util.Date

data class Block(
    val id: String,
    val blockId: String,
    val lmsWebUrl: String,
    val legacyWebUrl: String,
    val studentViewUrl: String,
    val type: BlockType,
    val displayName: String,
    val graded: Boolean,
    val studentViewData: StudentViewData?,
    val studentViewMultiDevice: Boolean,
    val blockCounts: BlockCounts,
    val descendants: List<String>,
    val descendantsType: BlockType,
    val completion: Double,
    val containsGatedContent: Boolean = false,
    val authorizationDenialReason: AuthorizationDenialReason,
    val downloadModel: DownloadModel? = null,
    val assignmentProgress: AssignmentProgress?,
    val due: Date?
) {
    val isDownloadable: Boolean
        get() {
            return studentViewData != null && studentViewData.encodedVideos?.hasDownloadableVideo == true
        }

    val downloadableType: FileType
        get() = when (type) {
            BlockType.VIDEO -> {
                FileType.VIDEO
            }

            else -> {
                FileType.UNKNOWN
            }
        }

    fun isDownloading(): Boolean {
        return downloadModel?.downloadedState == DownloadedState.DOWNLOADING ||
                downloadModel?.downloadedState == DownloadedState.WAITING
    }

    fun isDownloaded() = downloadModel?.downloadedState == DownloadedState.DOWNLOADED

    fun isGated() = containsGatedContent

    fun isCompleted() = completion == 1.0

    fun getFirstDescendantBlock(blocks: List<Block>): Block? {
        if (blocks.isEmpty()) return null
        descendants.forEach { descendant ->
            blocks.find { it.id == descendant }?.let { descendantBlock ->
                return descendantBlock
            }
        }
        return null
    }

    fun getDownloadsCount(blocks: List<Block>): Int {
        if (blocks.isEmpty()) return 0
        var count = 0
        descendants.forEach { id ->
            blocks.find { it.id == id }?.let { descendantBlock ->
                count += blocks.filter { descendantBlock.descendants.contains(it.id) && it.isDownloadable }.size
            }
        }
        return count
    }

    fun getVideoPreview(context: Context, isOnline: Boolean, offlineUrl: String?): VideoPreview? {
        return if (studentViewData?.encodedVideos?.hasYoutubeUrl == true) {
            val youtubeUrl = studentViewData.encodedVideos.youtube?.url ?: ""
            VideoPreview.createYoutubePreview(
                PreviewHelper.getYouTubeThumbnailUrl(youtubeUrl)
            )
        } else if (studentViewData?.encodedVideos?.hasVideoUrl == true) {
            val videoUrl = if (studentViewData.encodedVideos.videoUrl.isNotEmpty() && isOnline) {
                studentViewData.encodedVideos.videoUrl
            } else {
                offlineUrl ?: ""
            }
            val bitmap = PreviewHelper.getVideoFrameBitmap(
                context = context,
                isOnline = isOnline,
                videoUrl = videoUrl
            )
            bitmap?.let { VideoPreview.createEncodedVideoPreview(it) }
        } else {
            null
        }
    }
    val videoUrl: String?
        get() = if (studentViewData?.encodedVideos?.hasVideoUrl == true) {
            studentViewData.encodedVideos.videoUrl
        } else {
            studentViewData?.encodedVideos?.youtube?.url
        }
    fun isPaidContent(): Boolean =
        authorizationDenialReason == AuthorizationDenialReason.FEATURE_BASED_ENROLLMENTS

    val isVideoBlock get() = type == BlockType.VIDEO
    val isDiscussionBlock get() = type == BlockType.DISCUSSION
    val isHTMLBlock get() = type == BlockType.HTML
    val isProblemBlock get() = type == BlockType.PROBLEM
    val isOpenAssessmentBlock get() = type == BlockType.OPENASSESSMENT
    val isDragAndDropBlock get() = type == BlockType.DRAG_AND_DROP_V2
    val isWordCloudBlock get() = type == BlockType.WORD_CLOUD
    val isLTIConsumerBlock get() = type == BlockType.LTI_CONSUMER
    val isSurveyBlock get() = type == BlockType.SURVEY
}

data class StudentViewData(
    val onlyOnWeb: Boolean,
    val duration: Any,
    val transcripts: HashMap<String, String>?,
    val encodedVideos: EncodedVideos?,
    val topicId: String,
)

data class EncodedVideos(
    val youtube: VideoInfo?,
    var hls: VideoInfo?,
    var fallback: VideoInfo?,
    var desktopMp4: VideoInfo?,
    var mobileHigh: VideoInfo?,
    var mobileLow: VideoInfo?,
) {
    val hasDownloadableVideo: Boolean
        get() = isPreferredVideoInfo(hls) ||
                isPreferredVideoInfo(fallback) ||
                isPreferredVideoInfo(desktopMp4) ||
                isPreferredVideoInfo(mobileHigh) ||
                isPreferredVideoInfo(mobileLow)

    val hasNonYoutubeVideo: Boolean
        get() = mobileHigh?.url != null
                || mobileLow?.url != null
                || desktopMp4?.url != null
                || hls?.url != null
                || fallback?.url != null

    val videoUrl: String
        get() =  mobileLow?.url?.takeIf { it.isNotEmpty() }
            ?: fallback?.url?.takeIf { it.isNotEmpty() }
            ?: hls?.url?.takeIf { it.isNotEmpty() }
            ?: desktopMp4?.url?.takeIf { it.isNotEmpty() }
            ?: mobileHigh?.url?.takeIf { it.isNotEmpty() }
            ?: ""
    val hasVideoUrl: Boolean
        get() = videoUrl.isNotEmpty()

    val hasYoutubeUrl: Boolean
        get() = youtube?.url?.isNotEmpty() == true

    fun getPreferredVideoInfoForStreaming(preferredVideoStreaming: VideoQuality): VideoInfo {
        return when (preferredVideoStreaming) {
            VideoQuality.AUTO -> {
                listOfNotNull(
                    mobileLow,
                    mobileHigh,
                    desktopMp4,
                    hls,
                    youtube,
                    fallback,
                ).minBy { it.streamPriority }
            }

            VideoQuality.OPTION_720P -> {
                listOfNotNull(
                    desktopMp4,
                    mobileHigh,
                    mobileLow,
                    hls,
                    youtube,
                    fallback,
                ).first()
            }

            VideoQuality.OPTION_540P -> {
                listOfNotNull(
                    mobileHigh,
                    mobileLow,
                    desktopMp4,
                    hls,
                    youtube,
                    fallback,
                ).first()
            }

            VideoQuality.OPTION_360P -> {
                listOfNotNull(
                    mobileLow,
                    mobileHigh,
                    desktopMp4,
                    hls,
                    youtube,
                    fallback,
                ).first()
            }
        }
    }

    fun getPreferredVideoInfoForDownloading(preferredVideoQuality: VideoQuality): VideoInfo? {
        var preferredVideoInfo = when (preferredVideoQuality) {
            VideoQuality.OPTION_360P -> mobileLow
            VideoQuality.OPTION_540P -> mobileHigh
            VideoQuality.OPTION_720P -> desktopMp4
            else -> null
        }
        if (preferredVideoInfo == null) {
            preferredVideoInfo = getDefaultVideoInfoForDownloading()
        }
        return if (isPreferredVideoInfo(preferredVideoInfo)) {
            preferredVideoInfo
        } else {
            null
        }
    }

    private fun getDefaultVideoInfoForDownloading(): VideoInfo? {
        if (isPreferredVideoInfo(mobileLow)) {
            return mobileLow
        }
        if (isPreferredVideoInfo(mobileHigh)) {
            return mobileHigh
        }
        if (isPreferredVideoInfo(desktopMp4)) {
            return desktopMp4
        }
        fallback?.let {
            if (isPreferredVideoInfo(it) &&
                !VideoUtil.videoHasFormat(it.url, AppDataConstants.VIDEO_FORMAT_M3U8)
            ) {
                return fallback
            }
        }
        hls?.let {
            if (isPreferredVideoInfo(it)
            ) {
                return hls
            }
        }
        return null
    }

    private fun isPreferredVideoInfo(videoInfo: VideoInfo?): Boolean {
        return videoInfo != null &&
                URLUtil.isNetworkUrl(videoInfo.url) &&
                VideoUtil.isValidVideoUrl(videoInfo.url)
    }

}

data class VideoInfo(
    val url: String,
    val fileSize: Long,
    val streamPriority: Int,
)

data class BlockCounts(
    val video: Int,
)

enum class AuthorizationDenialReason(val rawValue: String) {
    FEATURE_BASED_ENROLLMENTS("Feature-based Enrollments"),
    UNKNOWN("Unknown");

    companion object {
        fun from(value: String?): AuthorizationDenialReason {
            return entries.find { it.rawValue == value } ?: UNKNOWN
        }
    }
}
