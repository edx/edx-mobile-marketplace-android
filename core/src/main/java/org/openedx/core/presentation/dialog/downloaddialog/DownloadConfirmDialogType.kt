package org.openedx.core.presentation.dialog.downloaddialog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class DownloadConfirmDialogType : Parcelable {
    DOWNLOAD_ON_CELLULAR, CONFIRM, REMOVE
}
