package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName

data class CountByAppNameModel(
    @SerializedName("discussion")
    var discussion: Int,
    @SerializedName("updates")
    var updates: Int,
    @SerializedName("grading")
    var grading: Int,
)
