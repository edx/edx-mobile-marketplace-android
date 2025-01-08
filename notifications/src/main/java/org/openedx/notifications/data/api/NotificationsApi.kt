package org.openedx.notifications.data.api

import org.openedx.notifications.data.model.InboxNotificationsResponse
import org.openedx.notifications.data.model.MarkNotificationReadBody
import org.openedx.notifications.data.model.NotificationsCountResponse
import org.openedx.notifications.data.model.NotificationsMarkResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationsApi {
    @GET(APIConstants.NOTIFICATION_COUNT)
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
}
