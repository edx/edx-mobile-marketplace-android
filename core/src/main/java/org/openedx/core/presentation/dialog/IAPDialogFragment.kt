package org.openedx.core.presentation.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.R
import org.openedx.core.domain.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.extension.parcelable
import org.openedx.core.extension.serializable
import org.openedx.core.extension.setFullScreen
import org.openedx.core.presentation.iap.IAPFlow
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.presentation.iap.IAPViewModel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.UnlockingAccessView
import org.openedx.core.ui.UpgradeErrorDialog
import org.openedx.core.ui.ValuePropUpgradeFeatures
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors

class IAPDialogFragment : DialogFragment() {

    private val iapViewModel by viewModel<IAPViewModel> {
        parametersOf(
            requireArguments().serializable<IAPFlow>(ARG_IAP_FLOW), PurchaseFlowData(
                screenName = requireArguments().getString(ARG_SCREEN_NAME, ""),
                courseId = requireArguments().getString(ARG_COURSE_ID, ""),
                courseName = requireArguments().getString(ARG_COURSE_NAME, ""),
                isSelfPaced = requireArguments().getBoolean(ARG_SELF_PACES, false),
                componentId = requireArguments().getString(ARG_COMPONENT_ID, ""),
                productInfo = requireArguments().parcelable<ProductInfo>(ARG_PRODUCT_INFO)
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val iapState by iapViewModel.uiState.collectAsState()
                val uiMessage by iapViewModel.uiMessage.collectAsState(null)
                val scaffoldState = rememberScaffoldState()

                val isFullScreenLoader =
                    iapState is IAPUIState.Loading && (iapState as IAPUIState.Loading).loaderType == IAPLoaderType.FULL_SCREEN

                Scaffold(modifier = Modifier.fillMaxSize(),
                    backgroundColor = MaterialTheme.appColors.background,
                    topBar = {
                        if (isFullScreenLoader.not()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    modifier = Modifier.clickable { onDismiss() },
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    bottomBar = {
                        if (isFullScreenLoader.not()) {
                            Box(modifier = Modifier.padding(all = 16.dp)) {
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

                                    iapState is IAPUIState.ProductData && TextUtils.isEmpty(
                                        iapViewModel.purchaseFlowData.formattedPrice
                                    ).not() -> {
                                        OpenEdXButton(modifier = Modifier.fillMaxWidth(),
                                            text = stringResource(
                                                id = R.string.iap_upgrade_price,
                                                iapViewModel.purchaseFlowData.formattedPrice!!,
                                            ),
                                            onClick = {
                                                iapViewModel.startPurchaseFlow()
                                            })
                                    }
                                }
                            }
                        }
                    }) { contentPadding ->

                    HandleUIMessage(
                        uiMessage = uiMessage,
                        scaffoldState = scaffoldState,
                        onDisplayed = {
                            if (iapState is IAPUIState.CourseDataUpdated) {
                                onDismiss()
                            }
                        })

                    when (iapState) {
                        is IAPUIState.PurchaseProduct -> {
                            iapViewModel.purchaseItem(requireActivity())
                        }

                        is IAPUIState.Error -> {
                            UpgradeErrorDialog(onDismiss = {
                                onDismiss()
                            }, onGetHelp = {
                                iapViewModel.showFeedbackScreen(
                                    requireActivity(),
                                    (iapState as IAPUIState.Error).feedbackErrorMessage
                                )
                                onDismiss()
                            })
                        }

                        is IAPUIState.Clear ->{
                            onDismiss()
                        }

                        else -> {}
                    }

                    if (isFullScreenLoader) {
                        UnlockingAccessView()
                    } else if (TextUtils.isEmpty(iapViewModel.purchaseFlowData.courseName).not()) {
                        ValuePropUpgradeFeatures(
                            Modifier.padding(contentPadding),
                            iapViewModel.purchaseFlowData.courseName!!
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setFullScreen(100)
    }

    private fun onDismiss() {
        iapViewModel.clearIAPFLow()
        dismiss()
    }

    companion object {
        private const val ARG_IAP_FLOW = "iap_flow"
        private const val ARG_SCREEN_NAME = "SCREEN_NAME"
        private const val ARG_COURSE_ID = "course_id"
        private const val ARG_COURSE_NAME = "course_name"
        private const val ARG_SELF_PACES = "self_paces"
        private const val ARG_COMPONENT_ID = "component_id"
        private const val ARG_PRODUCT_INFO = "product_info"

        fun newInstance(
            iapFlow: IAPFlow,
            screenName: String = "",
            courseId: String = "",
            courseName: String = "",
            isSelfPaced: Boolean = false,
            componentId: String? = null,
            productInfo: ProductInfo? = null
        ): IAPDialogFragment {
            val fragment = IAPDialogFragment()
            fragment.arguments = bundleOf(
                ARG_IAP_FLOW to iapFlow,
                ARG_SCREEN_NAME to screenName,
                ARG_COURSE_ID to courseId,
                ARG_COURSE_NAME to courseName,
                ARG_SELF_PACES to isSelfPaced,
                ARG_COMPONENT_ID to componentId,
                ARG_PRODUCT_INFO to productInfo
            )
            return fragment
        }
    }
}
