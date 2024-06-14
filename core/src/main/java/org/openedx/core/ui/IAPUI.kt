package org.openedx.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

@Composable
fun ValuePropUpgradeFeatures(modifier: Modifier = Modifier, courseName: String) {
    Column(
        modifier = modifier
            .padding(all = 16.dp)
    ) {
        Text(
            modifier = Modifier.padding(vertical = 32.dp),
            text = stringResource(
                id = R.string.iap_upgrade_course,
                courseName
            ),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        CheckmarkView(stringResource(id = R.string.iap_earn_certificate))
        CheckmarkView(stringResource(id = R.string.iap_unlock_access))
        CheckmarkView(stringResource(id = R.string.iap_full_access_course))
    }
}

@Composable
fun CheckmarkView(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(end = 16.dp),
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.appColors.certificateForeground
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.labelLarge
        )
    }
}

@Composable
fun UpgradeErrorDialog(onDismiss: () -> Unit, onGetHelp: () -> Unit) {
    AlertDialog(
        title = {
            Text(
                text = stringResource(id = R.string.iap_error_title),
                style = MaterialTheme.appTypography.titleMedium
            )
        },
        text = {
            Text(text = stringResource(id = R.string.iap_general_upgrade_error_message))
        },
        confirmButton = {
            OpenEdXButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                text = stringResource(id = R.string.core_cancel),
                onClick = onDismiss
            )
        },
        dismissButton = {
            OpenEdXButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                text = stringResource(id = R.string.iap_get_help),
                onClick = onGetHelp
            )
        },
        onDismissRequest = onDismiss
    )
}

@Composable
fun PurchasesFulfillmentCompletedDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        title = {
            Text(
                text = stringResource(id = R.string.iap_silent_course_upgrade_success_title),
                style = MaterialTheme.appTypography.titleMedium
            )
        },
        text = {
            Text(text = stringResource(id = R.string.iap_silent_course_upgrade_success_message))
        },
        confirmButton = {
            OpenEdXButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(4.dp),
                text = stringResource(id = R.string.iap_label_refresh_now),
                onClick = onConfirm
            )
        },
        dismissButton = {
            OpenEdXButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(4.dp),
                text = stringResource(id = R.string.iap_label_continue_without_update),
                onClick = onDismiss
            )
        },
        onDismissRequest = onDismiss
    )
}

@Preview
@Composable
private fun PreviewValuePropUpgradeFeatures() {
    ValuePropUpgradeFeatures(modifier = Modifier.background(Color.White), "Test Course")
}

@Preview
@Composable
private fun PreviewUpgradeErrorDialog() {
    UpgradeErrorDialog(onDismiss = {}, onGetHelp = {})
}

@Preview
@Composable
private fun PreviewPurchasesFulfillmentCompletedDialog() {
    PurchasesFulfillmentCompletedDialog(onConfirm = {}, onDismiss = {})
}
