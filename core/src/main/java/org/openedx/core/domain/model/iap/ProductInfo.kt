package org.openedx.core.domain.model.iap

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.ProductInfoDb

@Parcelize
data class ProductInfo(
    val courseSku: String,
    val storeSku: String,
    val lmsUSDPrice: Double,
) : Parcelable {

    fun mapToRoomEntity() = ProductInfoDb(
        courseSku = courseSku,
        storeSku = storeSku,
        lmsUSDPrice = lmsUSDPrice,
    )
}
