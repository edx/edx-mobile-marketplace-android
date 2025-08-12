package org.openedx.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.R
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isNotNullOrEmpty
import org.openedx.core.extension.toTitleCase
import org.openedx.core.presentation.iap.CourseTrack
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPErrorDialogType
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
fun ValuePropUpgradeFeatures(
    modifier: Modifier = Modifier,
    previewCertificate: Boolean,
    appName: String,
    fullName: String?,
    courseName: String,
    orgName: String,
    orgLogo: String?
) {
    Column(
        modifier = modifier
            .background(color = MaterialTheme.appColors.background)
            .verticalScroll(rememberScrollState())
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
        if (previewCertificate) {
            CertificatePreview(appName, fullName, courseName, orgName, orgLogo)
        }
    }
}

@Composable
fun CertificatePreview(
    appName: String,
    fullName: String?,
    courseName: String,
    orgName: String,
    orgLogo: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .padding(horizontal = 4.dp)
            .clip(shape = RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(width = 1.dp, Color.LightGray, shape = RoundedCornerShape(6.dp))
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .fillMaxHeight(),
            painter = painterResource(id = R.drawable.core_ic_certificate_preview_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )
        Row(modifier = Modifier.padding(all = 12.dp)) {
            Column(
                modifier = Modifier
                    .weight(0.7F)
                    .fillMaxHeight()
                    .padding(end = 36.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.iap_certificate_verified_text),
                    color = MaterialTheme.appColors.certificatePreviewHeading,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.appTypography.titleSmall.copy(fontStyle = FontStyle.Italic)
                )
                Text(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    text = stringResource(R.string.iap_certificate_text),
                    color = MaterialTheme.appColors.certificatePreviewHeading,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.appTypography.labelTiny.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    text = stringResource(R.string.iap_certificate_certify_message_1),
                    color = MaterialTheme.appColors.certificatePreviewMessage,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.appTypography.bodyTiny
                )

                Text(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    text = fullName?.toTitleCase() ?: "",
                    color = MaterialTheme.appColors.certificatePreviewHeading,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.appTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    text = stringResource(R.string.iap_certificate_certify_message_2),
                    color = MaterialTheme.appColors.certificatePreviewHeading,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.appTypography.bodyTiny
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    text = courseName,
                    color = MaterialTheme.appColors.certificatePreviewHeading,
                    textAlign = TextAlign.Start,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.appTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    text = stringResource(
                        R.string.iap_certificate_organization_message,
                        orgName,
                        appName
                    ),
                    color = MaterialTheme.appColors.certificatePreviewMessage,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.appTypography.bodyTiny
                )
                Spacer(modifier = Modifier.weight(1f))

                Row(modifier = Modifier.fillMaxWidth(0.9f)) {
                    Image(
                        modifier = Modifier
                            .width(38.dp)
                            .height(24.dp),
                        painter = painterResource(R.drawable.core_ic_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.appColors.certificatePreviewHeading),
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            modifier = Modifier.wrapContentSize(),
                            text = stringResource(R.string.iap_certificate_verified_certificate_text),
                            color = MaterialTheme.appColors.certificatePreviewMessage,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.appTypography.bodyTiny.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 4.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            modifier = Modifier.wrapContentSize(),
                            text = stringResource(R.string.iap_certificate_issued_date_text),
                            color = MaterialTheme.appColors.certificatePreviewMessage,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.appTypography.bodyTiny.copy(
                                fontSize = 4.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            modifier = Modifier.wrapContentSize(),
                            text = stringResource(R.string.iap_certificate_valid_certificate_id_label),
                            color = MaterialTheme.appColors.certificatePreviewMessage,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            style = MaterialTheme.appTypography.bodyTiny.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 4.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            modifier = Modifier.wrapContentSize(),
                            text = stringResource(R.string.iap_certificate_valid_certificate_id_sample),
                            color = MaterialTheme.appColors.certificatePreviewMessage,
                            maxLines = 1,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.appTypography.bodyTiny.copy(
                                fontSize = 4.sp
                            )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                AsyncImage(
                    modifier = Modifier
                        .height(28.dp)
                        .wrapContentWidth(),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(orgLogo)
                        .build(),
                    contentDescription = null,
                )

                SignatureInfo(stringResource(R.string.iap_certificate_preview_author_1))
                SignatureInfo(stringResource(R.string.iap_certificate_preview_author_2))
                SignatureInfo(stringResource(R.string.iap_certificate_preview_author_3))
            }
        }
    }
}

@Composable
fun SignatureInfo(name: String) {
    Spacer(modifier = Modifier.height(12.dp))
    Image(
        modifier = Modifier
            .height(16.dp)
            .wrapContentWidth(),
        painter = painterResource(id = R.drawable.core_ic_certificate_preview_signature),
        contentDescription = null,
        contentScale = ContentScale.FillHeight
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        modifier = Modifier.wrapContentSize(),
        text = name,
        color = MaterialTheme.appColors.certificatePreviewMessage,
        style = MaterialTheme.appTypography.bodyTiny
    )
    Spacer(modifier = Modifier.height(0.5.dp))
    Text(
        modifier = Modifier.wrapContentSize(),
        text = stringResource(R.string.iap_certificate_professor_text),
        color = MaterialTheme.appColors.textPrimaryLight,
        style = MaterialTheme.appTypography.bodyTiny
    )
    Spacer(modifier = Modifier.height(0.5.dp))
    Text(
        modifier = Modifier.wrapContentSize(),
        text = stringResource(R.string.iap_certificate_universityx_text),
        color = MaterialTheme.appColors.textPrimaryLight,
        style = MaterialTheme.appTypography.bodyTiny
    )
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
    courseName: String,
    price: String,
    expiryDate: String = "",
    selectedTrack: CourseTrack,
    onTrackSelection: (option: CourseTrack) -> Unit,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.appColors.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResource(
                id = R.string.iap_upgrade_course,
                courseName
            ),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.headlineSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.iap_track_selection_title),
            color = MaterialTheme.appColors.textPrimary,
            textAlign = TextAlign.Start,
            style = MaterialTheme.appTypography.titleLarge,
        )

        Spacer(modifier = Modifier.height(16.dp))

        CourseTrack.entries.forEach { option ->
            OptionCard(
                courseTrack = option,
                price = price,
                expiryDate = expiryDate,
                isSelected = selectedTrack == option,
                onClick = { onTrackSelection(option) },
            )
        }
    }
}

@Composable
fun OptionCard(
    courseTrack: CourseTrack,
    price: String = "",
    expiryDate: String = "",
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val width =
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) configuration.screenWidthDp
        else (configuration.screenWidthDp * 0.5).toInt()

    Card(
        modifier = Modifier
            .width(width.dp)
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
                contentDescription = stringResource(id = courseTrack.title, price),
                isSelected = isSelected,
                onClick = onClick,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = courseTrack.title, price),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (courseTrack == CourseTrack.FREE && expiryDate.isNotNullOrEmpty()) {
                    Text(
                        text = stringResource(id = R.string.core_label_expires, expiryDate),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(courseTrack.description, expiryDate),
                    style = MaterialTheme.appTypography.bodyMedium,
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

        IAPErrorDialogType.PURCHASE_FLOW_CONFLICT_ERROR_DIALOG,
        IAPErrorDialogType.CREATE_ORDER_BAD_REQUEST_ERROR_DIALOG,
        IAPErrorDialogType.CREATE_ORDER_FORBIDDEN_ERROR_DIALOG,
        IAPErrorDialogType.CREATE_ORDER_NOT_ACCEPTABLE_ERROR_DIALOG,
        IAPErrorDialogType.CREATE_ORDER_CONFLICT_ERROR_DIALOG,
        IAPErrorDialogType.CREATE_ORDER_GENERAL_ERROR_DIALOG,
        IAPErrorDialogType.COURSE_REFRESH_ERROR_DIALOG,
        IAPErrorDialogType.CONSUME_ERROR_DIALOG,
        IAPErrorDialogType.GENERAL_CONFLICT_ERROR_DIALOG,
            -> {
            CourseAlreadyPurchasedCreateOrderErrorDialog(
                description = stringResource(id = dialogType.messageResId),
                positiveText = stringResource(id = dialogType.positiveButtonResId),
                negativeText = stringResource(id = dialogType.negativeButtonResId),
                neutralText = stringResource(id = dialogType.neutralButtonResId),
                onPositiveClick = {
                    if (iapException.httpErrorCode == 409) {
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
fun CourseAlreadyPurchasedCreateOrderErrorDialog(
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
        ValuePropUpgradeFeatures(
            modifier = Modifier.background(Color.White),
            previewCertificate = true,
            appName = "Open edX",
            courseName = "Test Course",
            fullName = "john doe",
            orgName = "Google",
            orgLogo = "https://example/example.png"
        )
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
            onDismiss = {}
        )
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
private fun PreviewCourseAlreadyPurchasedCreateOrderErrorDialog() {
    OpenEdXTheme {
        CourseAlreadyPurchasedCreateOrderErrorDialog(
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


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(device = Devices.NEXUS_9, uiMode = Configuration.ORIENTATION_LANDSCAPE)
@Composable
fun TrackSelectionFeaturePreview() {
    OpenEdXTheme {
        TrackSelectionFeature(
            selectedTrack = CourseTrack.CERTIFICATE,
            courseName = "Test Course",
            price = "Free",
            onTrackSelection = { },
        )
    }
}
