package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class OfflineDownload(
    @SerializedName("file_url")
    var fileUrl: String?,
    @SerializedName("last_modified")
    var lastModified: String?,
    @SerializedName("file_size")
    var fileSize: Long?,
) {
    fun mapToDomain() = OfflineDownload(
        fileUrl = fileUrl ?: "",
        lastModified = lastModified,
        fileSize = fileSize ?: 0
    )

   /* fun mapToRoomEntity() = OfflineDownloadDb(
        fileUrl = fileUrl ?: "",
        lastModified = lastModified,
        fileSize = fileSize ?: 0
    )*/
}
