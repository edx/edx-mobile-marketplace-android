package org.openedx.core.ui.theme.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.openedx.core.R

@Composable
fun SignInLogoView() {
    val painter = if (isSystemInDarkTheme()) {
        painterResource(id = R.drawable.core_ic_logo_dark)
    } else {
        painterResource(id = R.drawable.core_ic_logo)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.padding(top = 40.dp)
        )
    }
}
