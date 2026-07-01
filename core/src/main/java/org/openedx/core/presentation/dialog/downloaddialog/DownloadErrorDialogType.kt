package org.openedx.core.presentation.dialog.downloaddialog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class DownloadErrorDialogType : Parcelable {
    NO_CONNECTION, WIFI_REQUIRED, DOWNLOAD_FAILED
}
