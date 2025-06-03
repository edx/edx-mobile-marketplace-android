package org.openedx.discussion.data.model.response

import com.google.gson.annotations.SerializedName

data class Reason(
    @SerializedName("code")
    val code: String,

    @SerializedName("label")
    val label: String,
)
