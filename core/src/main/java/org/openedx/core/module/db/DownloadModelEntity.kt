package org.openedx.core.module.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.extension.objectToString
import org.openedx.core.extension.stringToObject

@Entity(tableName = "download_model")
data class DownloadModelEntity(
    @PrimaryKey
    @ColumnInfo("id")
    val id: String,
    @ColumnInfo("title")
    val title: String,
    @ColumnInfo("size")
    val size: Long,
    @ColumnInfo("path")
    val path: String,
    @ColumnInfo("url")
    val url: String,
    @ColumnInfo("type")
    val type: String,
    @ColumnInfo("downloadedState")
    val downloadedState: String,
    @ColumnInfo("progress")
    val progress: Float?,
    @ColumnInfo("transcriptUrls")
    val transcriptUrls: String,
    @ColumnInfo("transcriptPaths")
    val transcriptPaths: String,
    @ColumnInfo("transcriptDownloadedStatus")
    val transcriptDownloadedStatus: String,
) {

    fun mapToDomain() = DownloadModel(
        id,
        title,
        size,
        path,
        url,
        FileType.valueOf(type),
        DownloadedState.valueOf(downloadedState),
        progress,
        stringToObject<Map<String, String>>(transcriptUrls) ?: emptyMap(),
        stringToObject<Map<String, String>>(transcriptPaths) ?: emptyMap(),
        TranscriptsDownloadedState.valueOf(transcriptDownloadedStatus),
    )

    companion object {

        fun createFrom(downloadModel: DownloadModel): DownloadModelEntity {
            with(downloadModel) {
                return DownloadModelEntity(
                    id,
                    title,
                    size,
                    path,
                    url,
                    type.name,
                    downloadedState.name,
                    progress,
                    objectToString(transcriptUrls),
                    objectToString(transcriptPaths),
                    transcriptDownloadedStatus.name
                )
            }
        }

    }

}
