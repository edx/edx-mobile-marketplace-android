package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.discovery.CertificateDb
import org.openedx.core.extension.isNotNullOrEmpty

@Parcelize
data class Certificate(
    val certificateURL: String?
) : Parcelable {
    fun isCertificateEarned() = certificateURL?.isNotNullOrEmpty()

    fun mapToRoomEntity() = CertificateDb(certificateURL)
}
