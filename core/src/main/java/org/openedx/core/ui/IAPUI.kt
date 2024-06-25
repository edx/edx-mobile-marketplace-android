package org.openedx.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
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
fun PriceLoadErrorDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    UpgradeErrorDialog(
        title = stringResource(id = R.string.iap_error_title),
        description = stringResource(id = R.string.iap_error_price_not_fetched),
        confirmText = stringResource(id = R.string.iap_label_refresh_now),
        onConfirm = onConfirm,
        dismissText = stringResource(id = R.string.core_cancel),
        onDismiss = onDismiss
    )
}

@Composable
fun CourseAlreadyPurchasedErrorDialog(
    onRefresh: () -> Unit,
    onGetHelp: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = {
            Text(
                text = stringResource(id = R.string.iap_error_title),
                style = MaterialTheme.appTypography.titleMedium
            )
        },
        text = { Text(text = stringResource(id = R.string.iap_course_already_paid_for_message)) },
        buttons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                OpenEdXButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    text = stringResource(id = R.string.iap_label_refresh_now),
                    onClick = onRefresh
                )

                OpenEdXButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    text = stringResource(id = R.string.core_contact_support),
                    onClick = onGetHelp
                )

                OpenEdXButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    text = stringResource(id = R.string.core_cancel),
                    onClick = onDismiss
                )
            }
        },
        onDismissRequest = {}
    )
}

@Composable
fun GeneralUpgradeErrorDialog(
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (description.isBlank()) {
        stringResource(id = R.string.iap_general_upgrade_error_message)
    }
    UpgradeErrorDialog(
        title = stringResource(id = R.string.iap_error_title),
        description = description,
        confirmText = stringResource(id = R.string.core_cancel),
        onConfirm = onConfirm,
        dismissText = stringResource(id = R.string.iap_get_help),
        onDismiss = onDismiss
    )
}

@Composable
fun UpgradeErrorDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit
) {

    AlertDialog(
        title = { Text(text = title, style = MaterialTheme.appTypography.titleMedium) },
        text = { Text(text = description) },
        confirmButton = {
            OpenEdXButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                text = confirmText,
                onClick = onConfirm
            )
        },
        dismissButton = {
            OpenEdXButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                text = dismissText,
                onClick = onDismiss
            )
        },
        onDismissRequest = onConfirm
    )
}

@Composable
fun CheckingPurchasesDialog() {
    Dialog(onDismissRequest = { }) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .background(
                    MaterialTheme.appColors.cardViewBackground,
                    MaterialTheme.appShapes.cardShape
                )
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(id = R.string.iap_checking_purchases),
                style = MaterialTheme.appTypography.titleMedium
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp),
                color = MaterialTheme.appColors.primary
            )
        }
    }
}

@Composable
fun FakePurchasesFulfillmentCompleted(onCancel: () -> Unit, onGetHelp: () -> Unit) {
    AlertDialog(
        title = {
            Text(
                text = stringResource(id = R.string.iap_title_purchases_restored),
                style = MaterialTheme.appTypography.titleMedium
            )
        },
        text = {
            Text(text = stringResource(id = R.string.iap_message_purchases_restored))
        },
        confirmButton = {
            OpenEdXButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(4.dp),
                text = stringResource(id = R.string.core_cancel),
                onClick = onCancel
            )
        },
        dismissButton = {
            OpenEdXButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(4.dp),
                text = stringResource(id = R.string.iap_get_help),
                onClick = onGetHelp
            )
        },
        onDismissRequest = onCancel
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
    UpgradeErrorDialog(
        title = "Error while Upgrading",
        description = "Description of the error",
        confirmText = "Confirm",
        onConfirm = {},
        dismissText = "Dismiss",
        onDismiss = {})
}

@Preview
@Composable
private fun PreviewPurchasesFulfillmentCompletedDialog() {
    PurchasesFulfillmentCompletedDialog(onConfirm = {}, onDismiss = {})
}

@Preview
@Composable
private fun PreviewCheckingPurchasesDialog() {
    CheckingPurchasesDialog()
}

@Preview
@Composable
private fun PreviewFakePurchasesFulfillmentCompleted() {
    FakePurchasesFulfillmentCompleted(onCancel = {}, onGetHelp = {})
}

@Preview
@Composable
private fun PreviewCourseAlreadyPurchasedErrorDialog() {
    CourseAlreadyPurchasedErrorDialog(onRefresh = {}, onGetHelp = {}, onDismiss = {})
}
