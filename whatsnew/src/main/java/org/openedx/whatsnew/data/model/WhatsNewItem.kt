package org.openedx.whatsnew.data.model

import android.content.Context
import com.google.gson.annotations.SerializedName
import org.openedx.whatsnew.domain.model.WhatsNewItem as DomainWhatsNewItem

data class WhatsNewItem(
    @SerializedName("version")
    val version: String,
    @SerializedName("messages")
    val messages: List<WhatsNewMessage>,
) {
    fun mapToDomain(context: Context) = DomainWhatsNewItem(
        version = version,
        messages = messages.map { it.mapToDomain(context) }
    )
}
