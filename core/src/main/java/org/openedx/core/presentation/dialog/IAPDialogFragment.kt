package org.openedx.core.presentation.dialog

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.R
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.extension.isNotNullOrEmpty
import org.openedx.core.extension.parcelable
import org.openedx.core.presentation.iap.CourseTrack
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.presentation.iap.IAPViewModel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IAPErrorDialog
import org.openedx.core.ui.OpenEdXBrandButton
import org.openedx.core.ui.TrackSelectionFeature
import org.openedx.core.ui.UnlockingAccessView
import org.openedx.core.ui.ValuePropUpgradeFeatures
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.utils.TimeUtils

class IAPDialogFragment : DialogFragment() {

    private val iapViewModel by viewModel<IAPViewModel> {
        parametersOf(
            requireArguments().parcelable<PurchaseFlowData>(ARG_PURCHASE_FLOW_DATA)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            Box(Modifier.safeDrawingPadding()) {
                OpenEdXTheme {
                    val iapState by iapViewModel.uiState.collectAsState()
                    val uiMessage by iapViewModel.uiMessage.collectAsState(null)
                    val scaffoldState = rememberScaffoldState()

                    var selectedOption by remember { mutableStateOf(CourseTrack.CERTIFICATE) }

                    val isFullScreenLoader =
                        (iapState as? IAPUIState.Loading)?.loaderType == IAPLoaderType.FULL_SCREEN
                    isCancelable = !isFullScreenLoader
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = MaterialTheme.appColors.background,
                        topBar = {
                            if (isFullScreenLoader.not()) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        modifier = Modifier.clickable { onDismiss() },
                                        painter = painterResource(id = R.drawable.core_ic_back),
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            if (isFullScreenLoader.not()) {
                                Box(modifier = Modifier.padding(all = 8.dp)) {
                                    when {
                                        (iapState is IAPUIState.Loading ||
                                                iapState is IAPUIState.PurchaseProduct ||
                                                iapState is IAPUIState.Error) -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                            }
                                        }

                                        iapState is IAPUIState.ProductData &&
                                                iapViewModel.purchaseData.formattedPrice.isNotNullOrEmpty() &&
                                                iapViewModel.purchaseData.iapFlow == IAPFlow.USER_INITIATED -> {
                                            if (iapViewModel.purchaseData.screenName == IAPFlowSource.TRACK_SELECTION.screen) {
                                                val buttonText =
                                                    if (selectedOption == CourseTrack.CERTIFICATE) {
                                                        stringResource(
                                                            id = R.string.iap_continue_to_payment,
                                                            iapViewModel.purchaseData.formattedPrice!!,
                                                        )
                                                    } else {
                                                        stringResource(id = R.string.iap_continue_with_free_track)
                                                    }
                                                OpenEdXBrandButton(
                                                    text = buttonText,
                                                    onClick = {
                                             if (selectedOption == CourseTrack.CERTIFICATE) {
                                                iapViewModel.startPurchaseFlow()
                                            } else {
                                                iapViewModel.onContinueToFreeTrackClicked()
                                                onDismiss()
                                            }
                                                    })
                                            } else {
                                                OpenEdXBrandButton(
                                                    text = stringResource(
                                                        id = R.string.iap_upgrade_price,
                                                        iapViewModel.purchaseData.formattedPrice!!,
                                                    ),
                                                    onClick = {
                                                        iapViewModel.startPurchaseFlow()
                                                    })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { contentPadding ->

                        HandleUIMessage(
                            uiMessage = uiMessage,
                            scaffoldState = scaffoldState,
                            onDisplayed = {
                                if (iapState is IAPUIState.CourseDataUpdated) {
                                    onDismiss()
                                }
                            }
                        )

                        when (iapState) {
                            is IAPUIState.PurchaseProduct -> {
                                iapViewModel.purchaseItem(requireActivity())
                            }

                            is IAPUIState.Error -> {
                                val iapException = (iapState as IAPUIState.Error).iapException
                                IAPErrorDialog(iapException = iapException, onIAPAction = { iapAction ->
                                    when (iapAction) {
                                        IAPAction.ACTION_RELOAD_PRICE -> {
                                            iapViewModel.logErrorAction(
                                                iapException.requestType.request,
                                                IAPAction.ACTION_RELOAD_PRICE.action
                                            )
                                            iapViewModel.loadPrice()
                                        }

                                        IAPAction.ACTION_CLOSE -> {
                                            iapViewModel.logErrorAction(
                                                iapException.requestType.request,
                                                IAPAction.ACTION_CLOSE.action
                                            )
                                            onDismiss()
                                        }

                                        IAPAction.ACTION_OK -> {
                                            iapViewModel.logErrorAction(
                                                iapException.requestType.request,
                                                IAPAction.ACTION_OK.action
                                            )
                                            onDismiss()
                                        }

                                        IAPAction.ACTION_REFRESH -> {
                                            iapViewModel.logErrorAction(
                                                iapException.requestType.request,
                                                IAPAction.ACTION_REFRESH.action
                                            )
                                            iapViewModel.refreshCourse()
                                        }

                                        IAPAction.ACTION_GET_HELP -> {
                                            iapViewModel.showFeedbackScreen(
                                                requireActivity(),
                                                iapException.requestType.request,
                                                iapException.getFormattedErrorMessage()
                                            )
                                            onDismiss()
                                        }

                                        IAPAction.ACTION_RETRY -> {
                                            iapViewModel.logErrorAction(
                                                iapException.requestType.request,
                                                IAPAction.ACTION_RETRY.action
                                            )
                                            if (iapException.requestType == IAPRequestType.CONSUME_CODE) {
                                                iapViewModel.retryToConsumeOrder()
                                            } else if (iapException.requestType == IAPRequestType.CREATE_ORDER_CODE) {
                                                iapViewModel.retryCreateOrder()
                                            }
                                        }

                                        else -> {
                                            // ignore
                                        }
                                    }
                                })
                            }

                            is IAPUIState.Clear -> {
                                onDismiss()
                            }

                            else -> {}
                        }

                        if (isFullScreenLoader) {
                            UnlockingAccessView()
                        } else if (TextUtils.isEmpty(iapViewModel.purchaseData.courseName).not()) {
                            if (iapViewModel.purchaseData.screenName == IAPFlowSource.TRACK_SELECTION.screen) {
                                val courseExpiresDate =
                                    iapViewModel.purchaseData.courseExpiresDate?.let {
                                        TimeUtils.getCourseAccessFormattedDate(
                                            LocalContext.current,
                                            it
                                        )
                                    } ?: ""
                                TrackSelectionFeature(
                                    modifier = Modifier.padding(contentPadding),
                                    courseName = iapViewModel.purchaseData.courseName!!,
                                    price = iapViewModel.purchaseData.formattedPrice ?: "",
                                    expiryDate = courseExpiresDate,
                                    selectedTrack = selectedOption,
                                    onTrackSelection = { option ->
                                        selectedOption = option
                                    },
                                )
                            } else {
                                ValuePropUpgradeFeatures(
                                    modifier = Modifier.padding(contentPadding),
                                    previewCertificate = iapViewModel.isCertificatePreviewEnabled,
                                    appName = iapViewModel.appData.appName,
                                    courseName = iapViewModel.purchaseData.courseName!!,
                                    learnerName = iapViewModel.user?.name,
                                    orgName = iapViewModel.purchaseData.orgName
                                        ?: iapViewModel.appData.appName,
                                    orgLogo = iapViewModel.purchaseData.orgLogo,
                                    onCertificatePreviewShown = {
                                        iapViewModel.logCertificatePreviewShown()
                                    },
                                )
                            }
                        } else {
                            // ignore
                        }
                    }
                }
            }

        }
    }

    override fun getTheme(): Int {
        return R.style.Theme_OpenEdX_IAPDialog
    }

    private fun onDismiss() {
        iapViewModel.clearIAPFLow()
        dismiss()
    }

    companion object {
        const val TAG = "IAPDialogFragment"
        private const val ARG_PURCHASE_FLOW_DATA = "purchase_flow_data"

        fun newInstance(purchaseFlowData: PurchaseFlowData): IAPDialogFragment {
            val fragment = IAPDialogFragment()
            fragment.arguments = bundleOf(ARG_PURCHASE_FLOW_DATA to purchaseFlowData)
            return fragment
        }
    }
}
