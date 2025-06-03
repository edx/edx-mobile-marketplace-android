package org.openedx.discussion.data.model.response

import com.google.gson.annotations.SerializedName

data class Blackout(
    @SerializedName("start")
    val start: String,

    @SerializedName("end")
    val end: String,
)
