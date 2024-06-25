package org.openedx.core.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

data class AppShapes(
    val material: Shapes,
    val buttonShape: RoundedCornerShape,
    val navigationButtonShape: RoundedCornerShape,
    val textFieldShape: RoundedCornerShape,
    val screenBackgroundShape: RoundedCornerShape,
    val cardShape: RoundedCornerShape,
    val screenBackgroundShapeFull: RoundedCornerShape,
    val courseImageShape: RoundedCornerShape,
    val dialogShape: RoundedCornerShape,
)

val MaterialTheme.appShapes: AppShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalShapes.current
