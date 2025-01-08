package org.openedx.notifications.data.api

import org.openedx.notifications.data.model.InboxNotificationsResponse
import org.openedx.notifications.data.model.MarkNotificationReadBody
import org.openedx.notifications.data.model.NotificationsConfiguration
import org.openedx.notifications.data.model.NotificationsCountResponse
import org.openedx.notifications.data.model.NotificationsMarkResponse
import org.openedx.notifications.data.model.NotificationsUpdateBody
import org.openedx.notifications.data.model.NotificationsUpdateResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationsApi {
    @GET(APIConstants.NOTIFICATIONS_COUNT)
    suspend fun getUnreadNotificationsCount(): NotificationsCountResponse

    @GET(APIConstants.NOTIFICATIONS_INBOX)
    suspend fun getInboxNotifications(
        @Query("app_name") appName: String,
        @Query("page") page: Int,
    ): InboxNotificationsResponse

    @PUT(APIConstants.NOTIFICATIONS_SEEN)
    suspend fun markNotificationsAsSeen(
        @Path("app_name") appName: String,
    ): NotificationsMarkResponse

    @PATCH(APIConstants.NOTIFICATION_READ)
    suspend fun markNotificationAsRead(
        @Body markNotification: MarkNotificationReadBody,
    ): NotificationsMarkResponse

    @GET(APIConstants.NOTIFICATIONS_CONFIGURATION)
    suspend fun fetchNotificationsConfiguration(): NotificationsConfiguration

    @POST(APIConstants.NOTIFICATION_UPDATE_CONFIGURATION)
    suspend fun updateNotificationsConfiguration(
        @Body notificationsUpdateBody: NotificationsUpdateBody,
    ): NotificationsUpdateResponse
}
