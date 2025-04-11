package org.openedx.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.openedx.core.R
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPErrorDialogType
import org.openedx.core.presentation.iap.TrackSelection
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
fun ValuePropUpgradeFeatures(modifier: Modifier = Modifier, courseName: String) {
    Column(
        modifier = modifier
            .background(color = MaterialTheme.appColors.background)
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(end = 16.dp),
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.appColors.successGreen
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
fun TrackSelectionFeature(
    modifier: Modifier = Modifier,
    price: String,
    selectedOption: TrackSelection,
    onOptionSelect: (option: TrackSelection) -> Unit,
) {

    Column(
        modifier = modifier
            .background(MaterialTheme.appColors.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(id = R.string.iap_track_selection_title),
            style = MaterialTheme.appTypography.headlineBold,
            color = MaterialTheme.appColors.textPrimary,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(24.dp))

        TrackSelection.entries.forEach { option ->
            OptionCard(
                trackSelection = option,
                price = if (option == TrackSelection.FREE) stringResource(R.string.iap_access_this_course_free) else price,
                isSelected = selectedOption == option,
                onClick = { onOptionSelect(option) },
            )
        }
    }
}

@Composable
fun OptionCard(
    trackSelection: TrackSelection,
    price: String = stringResource(R.string.iap_access_this_course_free),
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.appColors.primary
            else MaterialTheme.appColors.textFieldBorder
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            OpenEdxRadioButton(
                isSelected = isSelected,
                onClick = onClick,
            )

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(trackSelection.title),
                        color = MaterialTheme.appColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = price,
                        color = MaterialTheme.appColors.textPrimaryVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                    )
                }
                if (trackSelection == TrackSelection.FREE && trackSelection.accessExpires != null) {
                    Text(
                        text = stringResource(
                            id = R.string.core_label_expires,
                            trackSelection.accessExpires!!
                        ),
                        color = MaterialTheme.appColors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        trackSelection.description,
                        trackSelection.accessExpires ?: ""
                    ),
                    style = MaterialTheme.appTypography.bodySmall,
                    color = MaterialTheme.appColors.textPrimaryVariant,
                )
            }
        }
    }
}

@Composable
fun IAPErrorDialog(iapException: IAPException, onIAPAction: (IAPAction) -> Unit) {
    when (val dialogType = iapException.getIAPErrorDialogType()) {
        IAPErrorDialogType.PRICE_ERROR_DIALOG -> {
            UpgradeErrorDialog(
                title = stringResource(id = R.string.iap_error_title),
                description = stringResource(id = dialogType.messageResId),
                confirmText = stringResource(id = dialogType.positiveButtonResId),
                onConfirm = { onIAPAction(IAPAction.ACTION_RELOAD_PRICE) },
                dismissText = stringResource(id = dialogType.negativeButtonResId),
                onDismiss = { onIAPAction(IAPAction.ACTION_CLOSE) }
            )
        }

        IAPErrorDialogType.NO_SKU_ERROR_DIALOG -> {
            NoSkuErrorDialog(onConfirm = {
                onIAPAction(IAPAction.ACTION_OK)
            })
        }

        IAPErrorDialogType.ADD_TO_BASKET_NOT_ACCEPTABLE_ERROR_DIALOG,
        IAPErrorDialogType.CHECKOUT_NOT_ACCEPTABLE_ERROR_DIALOG,
        -> {
            UpgradeErrorDialog(
                title = stringResource(id = R.string.iap_error_title),
                description = stringResource(id = dialogType.messageResId),
                confirmText = stringResource(id = dialogType.positiveButtonResId),
                onConfirm = { onIAPAction(IAPAction.ACTION_REFRESH) },
                dismissText = stringResource(id = dialogType.negativeButtonResId),
                onDismiss = { onIAPAction(IAPAction.ACTION_CLOSE) }
            )
        }

        IAPErrorDialogType.EXECUTE_BAD_REQUEST_ERROR_DIALOG,
        IAPErrorDialogType.EXECUTE_FORBIDDEN_ERROR_DIALOG,
        IAPErrorDialogType.EXECUTE_NOT_ACCEPTABLE_ERROR_DIALOG,
        IAPErrorDialogType.EXECUTE_GENERAL_ERROR_DIALOG,
        IAPErrorDialogType.CONSUME_ERROR_DIALOG,
        -> {
            CourseAlreadyPurchasedExecuteErrorDialog(
                description = stringResource(id = dialogType.messageResId),
                positiveText = stringResource(id = dialogType.positiveButtonResId),
                negativeText = stringResource(id = dialogType.negativeButtonResId),
                neutralText = stringResource(id = dialogType.neutralButtonResId),
                onPositiveClick = {
                    if (iapException.httpErrorCode == 406) {
                        onIAPAction(IAPAction.ACTION_REFRESH)
                    } else {
                        onIAPAction(IAPAction.ACTION_RETRY)
                    }
                },
                onNegativeClick = {
                    onIAPAction(IAPAction.ACTION_GET_HELP)
                },
                onNeutralClick = {
                    onIAPAction(IAPAction.ACTION_CLOSE)
                }
            )
        }

        else -> {
            UpgradeErrorDialog(
                title = stringResource(id = R.string.iap_error_title),
                description = stringResource(id = dialogType.messageResId),
                confirmText = stringResource(id = dialogType.positiveButtonResId),
                onConfirm = { onIAPAction(IAPAction.ACTION_CLOSE) },
                dismissText = stringResource(id = dialogType.negativeButtonResId),
                onDismiss = { onIAPAction(IAPAction.ACTION_GET_HELP) }
            )
        }
    }
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
            OpenEdXTertiaryButton(
                text = stringResource(id = R.string.core_ok),
                onClick = onConfirm
            )
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismissRequest = {}
    )
}

@Composable
fun CourseAlreadyPurchasedExecuteErrorDialog(
    description: String,
    positiveText: String,
    negativeText: String,
    neutralText: String,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
    onNeutralClick: () -> Unit
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
                text = description,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        buttons = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OpenEdXTertiaryButton(
                    text = positiveText,
                    onClick = onPositiveClick
                )

                OpenEdXTertiaryButton(
                    text = negativeText,
                    onClick = onNegativeClick
                )

                OpenEdXTertiaryButton(
                    text = neutralText,
                    onClick = onNeutralClick
                )
            }
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismissRequest = {}
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
            OpenEdXTertiaryButton(
                text = confirmText,
                onClick = onConfirm
            )
        },
        dismissButton = {
            OpenEdXTertiaryButton(
                text = dismissText,
                onClick = onDismiss
            )
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismissRequest = {}
    )
}

@Composable
fun CheckingPurchasesDialog() {
    Dialog(
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismissRequest = {}
    ) {
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
            OpenEdXTertiaryButton(
                text = stringResource(id = R.string.core_cancel),
                onClick = onCancel
            )
        },
        dismissButton = {
            OpenEdXTertiaryButton(
                text = stringResource(id = R.string.iap_get_help),
                onClick = onGetHelp
            )
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismissRequest = {}
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
            OpenEdXTertiaryButton(
                text = stringResource(id = R.string.iap_label_refresh_now),
                onClick = onConfirm
            )
        },
        dismissButton = {
            OpenEdXTertiaryButton(
                text = stringResource(id = R.string.iap_label_continue_without_update),
                onClick = onDismiss
            )
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismissRequest = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewValuePropUpgradeFeatures() {
    OpenEdXTheme {
        ValuePropUpgradeFeatures(modifier = Modifier.background(Color.White), "Test Course")
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUpgradeErrorDialog() {
    OpenEdXTheme {
        UpgradeErrorDialog(
            title = "Error while Upgrading",
            description = "Description of the error",
            confirmText = "Confirm",
            onConfirm = {},
            dismissText = "Dismiss",
            onDismiss = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPurchasesFulfillmentCompletedDialog() {
    OpenEdXTheme {
        PurchasesFulfillmentCompletedDialog(onConfirm = {}, onDismiss = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCheckingPurchasesDialog() {
    OpenEdXTheme {
        CheckingPurchasesDialog()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFakePurchasesFulfillmentCompleted() {
    OpenEdXTheme {
        FakePurchasesFulfillmentCompleted(onCancel = {}, onGetHelp = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCourseAlreadyPurchasedExecuteErrorDialog() {
    OpenEdXTheme {
        CourseAlreadyPurchasedExecuteErrorDialog(
            description = stringResource(id = R.string.iap_course_not_fullfilled),
            positiveText = stringResource(id = R.string.iap_label_refresh_now),
            negativeText = stringResource(id = R.string.iap_get_help),
            neutralText = stringResource(id = R.string.core_cancel),
            onPositiveClick = {}, onNegativeClick = {}, onNeutralClick = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNoSkuErrorDialog() {
    OpenEdXTheme {
        NoSkuErrorDialog(onConfirm = {})
    }
}


@Preview
@Composable
fun TrackSelectionFeaturePreview() {
    OpenEdXTheme {
        TrackSelectionFeature(
            selectedOption = TrackSelection.CERTIFICATE,
            price = "Free",
            onOptionSelect = { },
        )
    }
}
