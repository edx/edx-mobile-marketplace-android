package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.CourseModeDB

@Parcelize
data class CourseMode(
    val slug: String?,
    val sku: String?,
    val androidSku: String?,
    val iosSku: String?,
    val minPrice: Double?,
    var storeSku: String?,
) : Parcelable {

    fun mapToRoomEntity() = CourseModeDB(
        slug = slug,
        sku = sku,
        androidSku = androidSku,
        iosSku = iosSku,
        minPrice = minPrice,
        storeSku = storeSku,
    )
}
