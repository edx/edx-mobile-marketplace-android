package org.openedx.auth.presentation.signin

import org.openedx.auth.data.model.AuthType
import org.openedx.core.domain.model.RegistrationField

/**
 * Data class to store UI state of the SignIn screen
 *
 * @param isFacebookAuthEnabled is Facebook auth enabled
 * @param isGoogleAuthEnabled is Google auth enabled
 * @param isMicrosoftAuthEnabled is Microsoft auth enabled
 * @param isSocialAuthEnabled are OAuth buttons visible
 * @param isLogistrationEnabled indicates if the pre-login experience is available
 * @param lastSignIn the last authentication type used
 * @param showProgress is progress visible
 * @param loginSuccess indicates if the login was successful
 * @param agreement contains the honor code with multiple hyperlinks to agreement URLs
 */
internal data class SignInUIState(
    val isFacebookAuthEnabled: Boolean = false,
    val isGoogleAuthEnabled: Boolean = false,
    val isMicrosoftAuthEnabled: Boolean = false,
    val isSocialAuthEnabled: Boolean = false,
    val isLogistrationEnabled: Boolean = false,
    val lastSignIn: AuthType = AuthType.PASSWORD,
    val showProgress: Boolean = false,
    val loginSuccess: Boolean = false,
    val agreement: RegistrationField? = null,
)
