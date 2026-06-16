package org.openedx.notifications.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin dependency injection module for notifications
 * Provides all necessary dependencies for course progress notifications
 */
fun NotificationModule(): Module = module {
    // Provide notification sender
    single {
        CourseProgressNotificationSender(androidContext())
    }

    // Provide the course progress notification manager
    single {
        CourseProgressNotificationManager(
            context = androidContext(),
            notificationSender = get()
        )
    }
}
