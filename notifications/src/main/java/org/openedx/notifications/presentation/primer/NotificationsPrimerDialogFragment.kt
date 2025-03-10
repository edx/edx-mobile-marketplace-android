package org.openedx.notifications.presentation.primer

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.DialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.OpenEdXBrandButton
import org.openedx.core.ui.OpenEdXTertiaryButton
import org.openedx.core.ui.OpenEdxAlertDialog
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.notifications.R
import org.openedx.notifications.presentation.NotificationsAnalyticsKey
import org.openedx.notifications.utils.PermissionUtils
import org.openedx.core.R as CoreR

class NotificationsPrimerDialogFragment : DialogFragment() {

    private val viewModel by viewModel<NotificationsPrimerViewModel>()

    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.enableDiscussionNotificationsPreference()
        } else {
            viewModel.dismissDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        isCancelable = false
        setContent {
            OpenEdXTheme {
                val uiState by viewModel.uiState.collectAsState()

                when (uiState) {
                    PrimerUIState.ShowDialog -> {
                        NotificationsPrimer(
                            onDismissRequest = {
                                viewModel.logPrimerActionEvent(NotificationsAnalyticsKey.NO_THANKS)
                                viewModel.dismissDialog()
                            },
                            onNotifyClick = {
                                viewModel.logPrimerActionEvent(NotificationsAnalyticsKey.NOTIFY_ME)
                                viewModel.hideDialog()
                                PermissionUtils.requestNotificationPermission(
                                    activity = requireActivity(),
                                    permissionLauncher = pushNotificationPermissionLauncher,
                                    onRationaleShown = {
                                        viewModel.showRationaleDialog()
                                    },
                                )
                            },
                        )
                    }

                    PrimerUIState.DismissDialog -> {
                        dismiss()
                    }

                    PrimerUIState.HideDialog -> {
                        // Do nothing
                    }

                    PrimerUIState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                        }
                    }

                    PrimerUIState.ShowRationaleDialog -> {
                        ShowRationalePermissionDialog(
                            onNegativeButtonClick = {
                                viewModel.dismissDialog()
                            },

                            onPositiveButtonClick = {
                                PermissionUtils.navigateToNotificationSettings(
                                    requireContext()
                                )
                                viewModel.dismissDialog()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsPrimer(
    onDismissRequest: () -> Unit,
    onNotifyClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val windowSize = rememberWindowSize()
    val isLandscapeMode =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || windowSize.isTablet
    val modifier = Modifier.fillMaxWidth(if (isLandscapeMode) 0.7f else 1f)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = !isLandscapeMode,
        ),
        content = {
            NotificationsPrimerView(
                modifier = modifier,
                onDismissRequest = onDismissRequest,
                onNotifyClick = onNotifyClick,
            )
        }
    )
}

@Composable
private fun NotificationsPrimerView(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onNotifyClick: () -> Unit,
) {
    Column(
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.appColors.notificationPrimerCardBackground)
                .padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    imageVector = Icons.Filled.Notifications,
                    tint = MaterialTheme.appColors.notificationPrimerBadge,
                    contentDescription = null
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(id = R.string.notification_primer_title),
                    color = MaterialTheme.appColors.notificationPrimerBadge,
                    style = MaterialTheme.appTypography.bodyMedium,
                    textAlign = TextAlign.Center
                )

            }
            Spacer(Modifier.size(16.dp))
            Text(
                text = stringResource(id = R.string.notification_primer_heading),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleLarge,
                textAlign = TextAlign.Start
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.appColors.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.notification_primer_description),
                color = MaterialTheme.appColors.textPrimaryVariant,
                style = MaterialTheme.appTypography.bodyLarge,
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.size(16.dp))
            NotificationsPrimerButtons(
                onDismissRequest = onDismissRequest,
                onNotifyClick = onNotifyClick,
            )
        }
    }
}

@Composable
fun NotificationsPrimerButtons(
    onDismissRequest: () -> Unit,
    onNotifyClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current

    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OpenEdXTertiaryButton(
                    text = stringResource(id = R.string.notification_primer_no_thanks),
                    onClick = onDismissRequest,
                )
                Spacer(Modifier.size(8.dp))
                OpenEdXBrandButton(
                    modifier = Modifier,
                    text = stringResource(id = R.string.notification_primer_notify_me),
                    onClick = onNotifyClick,
                )
            }
        }

        else -> {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OpenEdXBrandButton(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    text = stringResource(id = R.string.notification_primer_notify_me),
                    onClick = onNotifyClick,
                )
                Spacer(Modifier.size(8.dp))
                OpenEdXTertiaryButton(
                    text = stringResource(id = R.string.notification_primer_no_thanks),
                    onClick = onDismissRequest,
                )
            }
        }
    }
}

@Composable
private fun ShowRationalePermissionDialog(
    onPositiveButtonClick: () -> Unit = {},
    onNegativeButtonClick: () -> Unit = {},
) {
    OpenEdxAlertDialog(
        title = stringResource(id = CoreR.string.core_permission_dialog_title),
        message = stringResource(
            id = CoreR.string.core_permission_dialog_message,
            stringResource(id = R.string.notifications_notifications).lowercase()
        ),
        positiveBtnText = stringResource(id = CoreR.string.core_continue),
        negativeBtnText = stringResource(id = CoreR.string.core_cancel),
        positiveBtnAction = onPositiveButtonClick,
        negativeBtnAction = onNegativeButtonClick,
    )
}

@PreviewLightDark
@Composable
private fun NotificationsPrimerPreview() {
    OpenEdXTheme {
        NotificationsPrimerView(
            onDismissRequest = { },
            onNotifyClick = { }
        )
    }
}
