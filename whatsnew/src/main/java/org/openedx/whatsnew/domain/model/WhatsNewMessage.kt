package org.openedx.whatsnew.domain.model

import androidx.annotation.DrawableRes

data class WhatsNewMessage(
    @field:DrawableRes
    val image: Int,
    val title: String,
    val message: String
)
