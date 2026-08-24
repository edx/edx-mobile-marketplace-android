package org.openedx.course.domain.helper

import android.content.Context
import org.openedx.core.domain.model.Block
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.utils.VideoPreview



class VideoPreviewHelper(
    private val context: Context,
    private val networkConnection: NetworkConnection
) {


    fun getVideoPreview(block: Block, offlineUrl: String? = null): VideoPreview? {
        return block.getVideoPreview(
            context = context,
            isOnline = networkConnection.isOnline(),
            offlineUrl = offlineUrl
        )
    }


    fun getVideoPreviews(
        blocks: List<Block>,
        offlineUrls: Map<String, String>? = null
    ): Map<String, VideoPreview?> {
        return blocks.associate { block ->
            val offlineUrl = offlineUrls?.get(block.id)
            block.id to getVideoPreview(block, offlineUrl)
        }
    }


    fun getVideoPreviewWithId(
        blockId: String,
        block: Block,
        offlineUrl: String? = null
    ): Pair<String, VideoPreview?> {
        return blockId to getVideoPreview(block, offlineUrl)
    }
}
