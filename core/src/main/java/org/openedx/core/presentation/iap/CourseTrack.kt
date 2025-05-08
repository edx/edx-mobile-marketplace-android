package org.openedx.core.presentation.iap

import androidx.annotation.StringRes
import org.openedx.core.R

enum class CourseTrack(
    @StringRes val title: Int,
    @StringRes val description: Int,
) {
    CERTIFICATE(
        title = R.string.iap_earn_a_certificate,
        description = R.string.iap_earn_a_certificate_description,
    ),
    FREE(
        title = R.string.iap_access_this_course,
        description = R.string.iap_access_this_course_description,
    )
}
