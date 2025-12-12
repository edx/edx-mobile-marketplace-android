package org.openedx.course.presentation.unit.unlockcontent

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.R
import org.openedx.core.presentation.dialog.IAPDialogFragment
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPViewModel
import org.openedx.core.ui.CertificatePreview
import org.openedx.core.ui.IAPErrorDialog
import org.openedx.core.ui.OpenEdXBrandButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

class UnlockContentFragment : Fragment() {

    private val viewModel by viewModel<UnlockContentViewModel> {
        parametersOf(
            requireArguments().getString(ARG_BLOCK_ID, ""),
            requireArguments().getString(ARG_COURSE_ID, ""),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val uiState by viewModel.uiState.collectAsState()
                val uiEvent by viewModel.uiEvent.collectAsState(UnlockContentUIAction.None)

                GradedAssignmentLockedCard(
                    uiState = uiState,
                    viewModel = viewModel
                ) {
                    viewModel.startPurchaseFlow(requireActivity())
                }

                when (uiEvent) {
                    UnlockContentUIAction.FullScreenLoader -> {
                        IAPDialogFragment.newInstance(viewModel.purchaseData)
                            .show(
                                requireActivity().supportFragmentManager,
                                IAPDialogFragment.TAG
                            )
                    }

                    is UnlockContentUIAction.Error -> {
                        val iapException = (uiEvent as UnlockContentUIAction.Error).iapException
                        IAPErrorDialog(iapException = iapException, onIAPAction = { iapAction ->
                            when (iapAction) {

                                IAPAction.ACTION_RELOAD_PRICE -> {
                                    viewModel.reloadPrice(iapException)
                                }

                                IAPAction.ACTION_GET_HELP -> {
                                    viewModel.showFeedbackScreen(
                                        context,
                                        iapException.requestType.request,
                                        iapException.getFormattedErrorMessage()
                                    )
                                }

                                IAPAction.ACTION_REFRESH,
                                IAPAction.ACTION_RETRY -> {
                                    IAPDialogFragment.newInstance(viewModel.purchaseData)
                                        .show(
                                            requireActivity().supportFragmentManager,
                                            IAPDialogFragment.TAG
                                        )
                                }

                                else -> {
                                    viewModel.refreshIAPState()
                                }
                            }
                        })
                    }

                    UnlockContentUIAction.Clear -> {
                        viewModel.refreshIAPState()
                    }

                    else -> {

                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_BLOCK_ID = "blockId"

        fun newInstance(courseId: String, blockId: String): UnlockContentFragment {
            val fragment = UnlockContentFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_BLOCK_ID to blockId,
            )
            return fragment
        }
    }
}

@Composable
private fun GradedAssignmentLockedCard(
    modifier: Modifier = Modifier,
    uiState: UnlockContentUIState,
    viewModel: UnlockContentViewModel,
    onUpgradeClick: () -> Unit,
) {
    val purchaseData = remember { viewModel.purchaseData }

    val iapViewModel: IAPViewModel = koinViewModel(
        parameters = { parametersOf(purchaseData) }
    )
    Column(
        modifier = modifier
            .background(MaterialTheme.appColors.background)
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.appColors.background)
            ) {
                Column(
                    modifier = modifier.weight(0.45f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(id = R.string.iap_locked_content_title),
                            tint = MaterialTheme.appColors.textPrimary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.iap_locked_content_title),
                            style = MaterialTheme.appTypography.titleMedium,
                            color = MaterialTheme.appColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.iap_locked_content_description),
                        style = MaterialTheme.appTypography.bodyMedium,
                        color = MaterialTheme.appColors.textPrimary,
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    ValuePropContent(Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(10.dp))

                    when (uiState) {
                        UnlockContentUIState.Loading -> {
                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                        }

                        is UnlockContentUIState.ProductData -> {
                            OpenEdXBrandButton(
                                text = stringResource(
                                    id = R.string.iap_upgrade_price,
                                    uiState.formattedPrice,
                                ),
                                onClick = onUpgradeClick,
                            )
                        }

                        else -> {}
                    }
                }

                if (iapViewModel.isCertificatePreviewEnabled) {
                    CertificatePreview(
                        Modifier.weight(0.55f),
                        iapViewModel.appData.appName,
                        iapViewModel.purchaseData.courseName
                            ?: iapViewModel.purchaseData.courseName,
                        iapViewModel.user?.name.toString(),
                        viewModel.orgName
                            ?: viewModel.orgName.toString(),
                        iapViewModel.purchaseData.orgLogo ?: iapViewModel.purchaseData.orgLogo
                    )
                }
            }
        } else {
            Column(
                modifier = modifier
                    .background(MaterialTheme.appColors.background)
                    .padding(5.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(id = R.string.iap_locked_content_title),
                        tint = MaterialTheme.appColors.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.iap_locked_content_title),
                        style = MaterialTheme.appTypography.titleMedium,
                        color = MaterialTheme.appColors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.iap_locked_content_description),
                    style = MaterialTheme.appTypography.bodyMedium,
                    color = MaterialTheme.appColors.textPrimary,
                )

                Spacer(modifier = Modifier.height(12.dp))
                val widthModifier = Modifier.fillMaxWidth()
                ValuePropContent(widthModifier)
                Spacer(modifier = Modifier.height(10.dp))
                if (iapViewModel.isCertificatePreviewEnabled) {
                    CertificatePreview(
                        widthModifier,
                        iapViewModel.appData.appName,
                        iapViewModel.purchaseData.courseName
                            ?: iapViewModel.purchaseData.courseName,
                        iapViewModel.user?.name.toString(),
                        viewModel.orgName
                            ?: viewModel.orgName.toString(),
                        iapViewModel.purchaseData.orgLogo ?: iapViewModel.purchaseData.orgLogo
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                when (uiState) {
                    UnlockContentUIState.Loading -> {
                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                    }

                    is UnlockContentUIState.ProductData -> {
                        OpenEdXBrandButton(
                            text = stringResource(
                                id = R.string.iap_upgrade_price,
                                uiState.formattedPrice,
                            ),
                            onClick = onUpgradeClick,
                        )
                        Spacer(modifier = Modifier.height(50.dp))
                    }

                    else -> {}
                }
            }

        }
    }
}

@Composable
fun ValuePropContent(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 1.dp),
    ) {
        UpgradeBenefit(stringResource(id = R.string.iap_earn_certificate))
        UpgradeBenefit(stringResource(id = R.string.iap_unlock_access))
        UpgradeBenefit(stringResource(id = R.string.iap_full_access_course))
    }
}
@Composable
private fun UpgradeBenefit(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(vertical = 3.dp)
            .fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = text,
            tint = MaterialTheme.appColors.successGreen,
            modifier = Modifier
                .size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.appTypography.bodyMedium,
            color = MaterialTheme.appColors.textPrimary
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GradedAssignmentLockedCardPreview() {
    OpenEdXTheme {
        GradedAssignmentLockedCard(
            uiState = UnlockContentUIState.ProductData(formattedPrice = "$9.99"),
            viewModel = TODO()
        ) {}
    }
}
