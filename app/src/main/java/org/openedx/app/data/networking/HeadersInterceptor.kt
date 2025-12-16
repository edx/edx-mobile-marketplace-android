package org.openedx.app.data.networking

import okhttp3.Interceptor
import okhttp3.Response
import org.openedx.app.BuildConfig
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.presentation.global.AppData

class HeadersInterceptor(
    private val appData: AppData,
    private val config: Config,
    private val preferencesManager: CorePreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder().apply {
                    val token = preferencesManager.accessToken

                    if (token.isNotEmpty()) {
                        addHeader("Authorization", "${config.getAccessTokenType()} $token")
                    }

                    addHeader("Accept", "application/json")

                    val httpAgent = System.getProperty("http.agent") ?: ""
                    addHeader(
                        "User-Agent",
                        httpAgent + " " +
                                context.getString(R.string.app_name) + "/" +
                                BuildConfig.APPLICATION_ID + "/" +
                                BuildConfig.VERSION_NAME
                    )
                    addHeader("User-Agent", "$httpAgent ${appData.versionName}")
                }.build()
        )
    }
}
