package org.openedx.auth.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.auth.R
import org.openedx.auth.data.model.AuthType
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
internal fun SocialAuthView(
    modifier: Modifier = Modifier,
    isGoogleAuthEnabled: Boolean = true,
    isFacebookAuthEnabled: Boolean = true,
    isMicrosoftAuthEnabled: Boolean = true,
    lastSignIn: AuthType = AuthType.PASSWORD,
    isSignIn: Boolean = false,
    onEvent: (AuthType) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            modifier = Modifier
                .testTag("txt_social_continue_with"),
            text = stringResource(id = R.string.auth_continue_with),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleSmall
        )

        SocialAuthButtons(
            lastSignIn = lastSignIn,
            isGoogleAuthEnabled = isGoogleAuthEnabled,
            isFacebookAuthEnabled = isFacebookAuthEnabled,
            isMicrosoftAuthEnabled = isMicrosoftAuthEnabled,
            isSignIn = isSignIn,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun SocialAuthButtons(
    lastSignIn: AuthType,
    isGoogleAuthEnabled: Boolean,
    isFacebookAuthEnabled: Boolean,
    isMicrosoftAuthEnabled: Boolean,
    isSignIn: Boolean,
    onEvent: (AuthType) -> Unit,
) {
    val enabledAuthTypes = mutableListOf<AuthType>().apply {
        if (isGoogleAuthEnabled && (!isSignIn || lastSignIn != AuthType.GOOGLE)) {
            add(AuthType.GOOGLE)
        }
        if (isMicrosoftAuthEnabled && (!isSignIn || lastSignIn != AuthType.MICROSOFT)) {
            add(AuthType.MICROSOFT)
        }
        if (isFacebookAuthEnabled && (!isSignIn || lastSignIn != AuthType.FACEBOOK)) {
            add(AuthType.FACEBOOK)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSignIn && lastSignIn != AuthType.PASSWORD) {
            Text(
                modifier = Modifier
                    .testTag("txt_last_sign_in_with")
                    .padding(end = 12.dp),
                text = stringResource(id = R.string.auth_last_sign_in),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodySmall,
                textAlign = TextAlign.Center
            )
            SocialAuthButton(isSignIn = true, authType = lastSignIn, onEvent = onEvent)
            if (enabledAuthTypes.size > 1) {
                Divider(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 20.dp)
                        .width(1.dp)
                        .height(42.dp),
                    color = MaterialTheme.appColors.socialAuthDivider
                )
            }
        }

        enabledAuthTypes.forEach { authType ->
            SocialAuthButton(isSignIn = isSignIn, authType = authType, onEvent = onEvent)
        }
    }
}


@Composable
private fun SocialAuthButton(
    isSignIn: Boolean,
    authType: AuthType,
    onEvent: (AuthType) -> Unit,
) {
    val (iconRes, descriptionRes) = when (authType) {
        AuthType.GOOGLE -> Pair(
            R.drawable.ic_auth_google,
            if (isSignIn) R.string.auth_google else R.string.auth_continue_google
        )

        AuthType.FACEBOOK -> Pair(
            R.drawable.ic_auth_facebook,
            if (isSignIn) R.string.auth_facebook else R.string.auth_continue_facebook
        )

        AuthType.MICROSOFT -> Pair(
            R.drawable.ic_auth_microsoft,
            if (isSignIn) R.string.auth_microsoft else R.string.auth_continue_microsoft
        )

        AuthType.PASSWORD -> return
    }

    IconButton(
        modifier = Modifier
            .padding(end = 16.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.appColors.textFieldHint,
                shape = MaterialTheme.appShapes.socialAuthButtonShape
            )
            .size(42.dp),
        onClick = { onEvent(authType) }
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(id = descriptionRes),
            tint = Color.Unspecified
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SocialAuthViewPreview() {
    OpenEdXTheme {
        SocialAuthView(
            isGoogleAuthEnabled = true,
            isFacebookAuthEnabled = true,
            isMicrosoftAuthEnabled = true,
            lastSignIn = AuthType.GOOGLE,
            isSignIn = true,
            onEvent = {}
        )
    }
}
