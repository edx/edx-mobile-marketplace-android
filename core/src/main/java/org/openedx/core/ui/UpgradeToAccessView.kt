package org.openedx.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
fun UpgradeToAccessView(
    modifier: Modifier = Modifier,
    type: UpgradeToAccessViewType = UpgradeToAccessViewType.DASHBOARD,
    iconPadding: PaddingValues = PaddingValues(end = 16.dp),
    padding: PaddingValues = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
    onClick: () -> Unit,
) {
    val shape: Shape
    var primaryIcon = Icons.Filled.Lock
    var textColor = MaterialTheme.appColors.primaryButtonText
    var backgroundColor = MaterialTheme.appColors.primaryButtonBackground
    var secondaryIcon: @Composable () -> Unit = {
        Icon(
            modifier = Modifier
                .padding(start = 16.dp),
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = textColor
        )
    }
    when (type) {
        UpgradeToAccessViewType.DASHBOARD -> {
            shape = RoundedCornerShape(
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        }

        UpgradeToAccessViewType.COURSE -> {
            shape = MaterialTheme.appShapes.buttonShape
        }

        UpgradeToAccessViewType.GALLERY -> {
            primaryIcon = Icons.Filled.EmojiEvents
            textColor = MaterialTheme.appColors.textDark
            shape = RectangleShape
            backgroundColor = textColor.copy(0.05f)
            secondaryIcon = {
                Icon(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(16.dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = textColor
                )
            }
        }
    }
    Row(
        modifier = modifier
            .clip(shape = shape)
            .fillMaxWidth()
            .background(color = backgroundColor)
            .clickable {
                onClick()
            }
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(iconPadding),
            imageVector = primaryIcon,
            contentDescription = null,
            tint = textColor
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.iap_upgrade_access_course),
            color = textColor,
            style = MaterialTheme.appTypography.labelLarge
        )
        secondaryIcon()
    }
}

enum class UpgradeToAccessViewType {
    GALLERY,
    DASHBOARD,
    COURSE,
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun UpgradeToAccessViewPreview(
    @PreviewParameter(UpgradeToAccessViewTypeParameterProvider::class) type: UpgradeToAccessViewType
) {
    OpenEdXTheme {
        UpgradeToAccessView(type = type) {}
    }
}

private class UpgradeToAccessViewTypeParameterProvider :
    PreviewParameterProvider<UpgradeToAccessViewType> {
    override val values = sequenceOf(
        UpgradeToAccessViewType.DASHBOARD,
        UpgradeToAccessViewType.COURSE,
        UpgradeToAccessViewType.GALLERY,
    )
}
