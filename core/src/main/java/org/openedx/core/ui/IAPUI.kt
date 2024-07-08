package org.openedx.core.ui

import android.text.TextUtils
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
        confirmText = stringResource(id = R.string.core_error_try_again),
        onConfirm = onConfirm,
        dismissText = stringResource(id = R.string.core_cancel),
        onDismiss = onDismiss
    )
}

@Composable
fun NoSkuErrorDialog(
    onConfirm: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier
            .background(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.cardShape
            )
            .padding(bottom = 8.dp),
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.background,
        title = {
            Text(
                text = stringResource(id = R.string.iap_error_title),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.iap_error_price_not_fetched),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        confirmButton = {
            OpenEdXButton(
                modifier = Modifier.wrapContentSize(),
                text = stringResource(id = R.string.core_ok),
                onClick = onConfirm
            )
        },
        onDismissRequest = onConfirm
    )
}

@Composable
fun CourseAlreadyPurchasedErrorDialog(onRefresh: () -> Unit, onDismiss: () -> Unit) {
    UpgradeErrorDialog(
        title = stringResource(id = R.string.iap_error_title),
        description = stringResource(id = R.string.iap_course_already_paid_for_message),
        confirmText = stringResource(id = R.string.iap_label_refresh_now),
        onConfirm = onRefresh,
        dismissText = stringResource(id = R.string.core_cancel),
        onDismiss = onDismiss
    )
}

@Composable
fun CourseAlreadyPurchasedExecuteErrorDialog(
    confirmText: String = "",
    description: String = "",
    onRefresh: () -> Unit,
    onGetHelp: () -> Unit,
    onDismiss: () -> Unit
) {
    var genConfirmText = confirmText
    if (TextUtils.isEmpty(confirmText)) {
        genConfirmText = stringResource(id = R.string.iap_refresh_to_retry)
    }

    var genDescription = description
    if (TextUtils.isEmpty(genDescription)) {
        genDescription = stringResource(id = R.string.iap_course_not_fullfilled)
    }

    AlertDialog(
        modifier = Modifier
            .background(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.cardShape
            )
            .padding(bottom = 8.dp),
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.background,
        title = {
            Text(
                text = stringResource(id = R.string.iap_error_title),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
            )
        },
        text = {
            Text(
                text = genDescription,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        buttons = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                OpenEdXButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    text = genConfirmText,
                    onClick = onRefresh
                )

                OpenEdXButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    text = stringResource(id = R.string.core_contact_support),
                    onClick = onGetHelp
                )

                OpenEdXButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
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
    var genDescription = description
    if (description.isBlank()) {
        genDescription = stringResource(id = R.string.iap_general_upgrade_error_message)
    }
    UpgradeErrorDialog(
        title = stringResource(id = R.string.iap_error_title),
        description = genDescription,
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
        modifier = Modifier
            .background(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.cardShape
            )
            .padding(bottom = 8.dp),
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.background,
        title = {
            Text(
                text = title,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
            )
        },
        text = {
            Text(
                text = description,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        confirmButton = {
            OpenEdXButton(
                modifier = Modifier.wrapContentSize(),
                text = confirmText,
                onClick = onConfirm
            )
        },
        dismissButton = {
            OpenEdXButton(
                modifier = Modifier.wrapContentSize(),
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
        modifier = Modifier
            .background(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.cardShape
            )
            .padding(end = 8.dp, bottom = 8.dp),
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.background,
        title = {
            Text(
                text = stringResource(id = R.string.iap_title_purchases_restored),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.iap_message_purchases_restored),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium,
            )
        },
        confirmButton = {
            OpenEdXButton(
                modifier = Modifier.wrapContentSize(),
                text = stringResource(id = R.string.core_cancel),
                onClick = onCancel
            )
        },
        dismissButton = {
            OpenEdXButton(
                modifier = Modifier.wrapContentSize(),
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
        modifier = Modifier
            .background(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.cardShape
            )
            .padding(end = 8.dp, bottom = 8.dp),
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.background,
        title = {
            Text(
                text = stringResource(id = R.string.iap_silent_course_upgrade_success_title),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.iap_silent_course_upgrade_success_message),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium,
            )
        },
        confirmButton = {
            OpenEdXButton(
                modifier = Modifier.wrapContentSize(),
                text = stringResource(id = R.string.iap_label_refresh_now),
                onClick = onConfirm
            )
        },
        dismissButton = {
            OpenEdXButton(
                modifier = Modifier.wrapContentSize(),
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
    CourseAlreadyPurchasedErrorDialog(onRefresh = {}, onDismiss = {})
}

@Preview
@Composable
private fun PreviewCourseAlreadyPurchasedExecuteErrorDialog() {
    CourseAlreadyPurchasedExecuteErrorDialog(onRefresh = {}, onGetHelp = {}, onDismiss = {})
}

@Preview
@Composable
private fun PreviewNoSkuErrorDialog() {
    NoSkuErrorDialog(onConfirm = {})
}
