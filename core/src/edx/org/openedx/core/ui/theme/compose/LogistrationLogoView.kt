package org.openedx.core.ui.theme.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.openedx.core.R

@Composable
fun LogistrationLogoView() {
    val painter = if (isSystemInDarkTheme()) {
        painterResource(id = R.drawable.core_ic_logo_dark)
    } else {
        painterResource(id = R.drawable.core_ic_logo)
    }
    Image(
        modifier = Modifier
            .padding(top = 64.dp, bottom = 20.dp)
            .wrapContentWidth(),
        painter = painter,
        contentDescription = null,
    )
}
