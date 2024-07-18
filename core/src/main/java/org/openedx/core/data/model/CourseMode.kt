package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.EnrollmentMode
import kotlin.math.ceil
import org.openedx.core.domain.model.CourseMode as DomainCourseMode

/**
 * Data class representing the mode of a course ("audit, verified etc"), with various attributes
 * related to its identification and pricing.
 * */
data class CourseMode(
    @SerializedName("slug")
    val slug: String?,
    @SerializedName("sku")
    val sku: String?,
    @SerializedName("android_sku")
    val androidSku: String?,
    @SerializedName("ios_sku")
    val iosSku: String?,
    @SerializedName("min_price")
    val minPrice: Double?,
    var storeSku: String?,
) {
    fun mapToDomain() = DomainCourseMode(
        slug = slug,
        sku = sku,
        androidSku = androidSku,
        iosSku = iosSku,
        minPrice = minPrice,
        storeSku = storeSku
    )

    fun isVerifiedMode(): Boolean {
        return EnrollmentMode.VERIFIED.toString().equals(slug, ignoreCase = true)
    }

    fun setStoreProductSku(storeProductPrefix: String) {
        val ceilPrice = minPrice
            ?.let { ceil(it).toInt() }
            ?.takeIf { it > 0 }

        if (storeProductPrefix.isNotBlank() && ceilPrice != null) {
            storeSku = "$storeProductPrefix$ceilPrice"
        }
    }
}
