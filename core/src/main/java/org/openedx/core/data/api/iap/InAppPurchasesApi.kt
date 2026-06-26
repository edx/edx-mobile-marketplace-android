package org.openedx.core.data.api.iap

import org.openedx.core.data.model.iap.CreateOrderResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface InAppPurchasesApi {

    @FormUrlEncoded
    @POST("iap/create-order/")
    suspend fun createOrder(
        @Field("course_run_key") courseId: String,
        @Field("currency_code") currencyCode: String,
        @Field("price") price: Double,
        @Field("payment_processor") paymentProcessor: String,
        @Field("purchase_token") purchaseToken: String,
    ): Response<CreateOrderResponse>

    @FormUrlEncoded
    @POST("iap/create-program-order/")
    suspend fun createProgramOrder(
        @Field("program_uuid") programUuid: String,
        @Field("currency_code") currencyCode: String,
        @Field("price") price: Double,
        @Field("payment_processor") paymentProcessor: String,
        @Field("purchase_token") purchaseToken: String,
    ): Response<CreateOrderResponse>
}
