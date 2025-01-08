package org.openedx.profile.domain.model

import org.openedx.core.domain.model.AgreementUrls

/**
 * @param isIAPEnabled In App Purchase is enabled or not
 * @param agreementUrls User agreement urls
 * @param faqUrl FAQ url
 * @param feedbackFormUrl URL of the learner feedback form
 * @param supportEmail Email address of support
 * @param versionName Version of the application (1.0.0)
 * @param isPushNotificationsEnabled Push Notifications is enabled or not
 */
data class Configuration(
    val isIAPEnabled: Boolean,
    val agreementUrls: AgreementUrls,
    val faqUrl: String,
    val feedbackFormUrl: String,
    val supportEmail: String,
    val versionName: String,
    val isPushNotificationsEnabled: Boolean,
)
