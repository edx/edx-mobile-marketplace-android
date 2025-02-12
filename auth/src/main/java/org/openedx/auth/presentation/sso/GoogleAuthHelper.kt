package org.openedx.auth.presentation.sso

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.domain.model.SocialAuthResponse
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger

class GoogleAuthHelper(private val context: Context, private val config: Config) {

    private val logger = Logger(TAG)

    private fun getAuthToken(activityContext: Activity, name: String): String {
        return GoogleAuthUtil.getToken(
            activityContext,
            Account(name, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE),
            SCOPE
        )
    }

    private suspend fun getCredentials(activityContext: Activity): GoogleIdTokenCredential {
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption =
            GetSignInWithGoogleOption.Builder(config.getGoogleConfig().clientId).build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(
            request = request,
            context = activityContext,
        )
        return getGoogleIdToken(result.credential)
    }

    private fun getGoogleIdToken(credential: Credential): GoogleIdTokenCredential {
        return when {
            credential is GoogleIdTokenCredential -> {
                parseToken(credential.data)
            }

            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                parseToken(credential.data)
            }

            else -> {
                logger.e { "Unknown credential type: {${credential.type}}" }
                throw Exception("Unknown credential type: {${credential.type}}")
            }
        }
    }

    private fun parseToken(data: Bundle): GoogleIdTokenCredential =
        GoogleIdTokenCredential.createFrom(data)

    @WorkerThread
    suspend fun socialAuth(activityContext: Activity): SocialAuthResponse {
        return getCredentials(activityContext).let { credentials ->
            if (credentials.id.isNotBlank()) {
                val token = getAuthToken(activityContext, credentials.id)
                logger.d { token }
                SocialAuthResponse(
                    accessToken = token,
                    name = credentials.displayName.orEmpty(),
                    email = credentials.id,
                    authType = AuthType.GOOGLE,
                )
            } else {
                throw Exception("Unknown credential: {$credentials}")
            }
        }
    }

    fun isGoogleAuthEnabled() =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    private companion object {
        const val TAG = "GoogleAuthHelper"
        const val SCOPE = "oauth2: https://www.googleapis.com/auth/userinfo.email"
    }
}
