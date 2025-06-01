package org.openedx.core.data.model.iap

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.iap.CreateOrderResponse
import org.openedx.core.domain.model.iap.CreateOrderResponse as CreateOrderResponseDomain

data class CreateOrderResponse(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("order_number") val orderNumber: String,
) {
    fun mapToDomain(): CreateOrderResponse {
        return CreateOrderResponseDomain
    }
}
