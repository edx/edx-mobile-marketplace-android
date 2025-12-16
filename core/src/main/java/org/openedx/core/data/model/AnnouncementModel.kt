package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.AnnouncementModel as DomainAnnouncementModel

data class AnnouncementModel(
    @SerializedName("date")
    val date: String,
    @SerializedName("content")
    val content: String,
) {
    fun mapToDomain() = org.openedx.core.domain.model.AnnouncementModel(
        date,
        content
    )
}
