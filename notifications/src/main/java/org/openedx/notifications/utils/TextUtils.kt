package org.openedx.notifications.utils

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.text.HtmlCompat
import org.openedx.notifications.R
import org.openedx.notifications.domain.model.NotificationItem
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit


object TextUtils {

    fun dateToRelativeTimeString(context: Context, date: Date?): String {
        val inputDate = Calendar.getInstance().apply { time = date ?: return "" }
        val currentDate = Calendar.getInstance()

        val difference = currentDate.timeInMillis - inputDate.timeInMillis

        return when {
            difference == 0L -> {
                context.getString(
                    R.string.notification_now
                )
            }

            difference < TimeUnit.MINUTES.toMillis(1) -> {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(difference).toInt()
                context.resources.getQuantityString(
                    R.plurals.notifications_date_format_seconds,
                    seconds,
                    seconds
                )
            }

            difference < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(difference).toInt()
                context.resources.getQuantityString(
                    R.plurals.notifications_date_format_minutes,
                    minutes,
                    minutes
                )
            }

            difference < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(difference).toInt()
                context.resources.getQuantityString(
                    R.plurals.notifications_date_format_hours,
                    hours,
                    hours
                )
            }

            difference < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(difference).toInt()
                context.resources.getQuantityString(
                    R.plurals.notifications_date_format_days,
                    days,
                    days
                )
            }

            difference < TimeUnit.DAYS.toMillis(30) -> {
                val weeks = (TimeUnit.MILLISECONDS.toDays(difference) / 7).toInt()
                context.resources.getQuantityString(
                    R.plurals.notifications_date_format_weeks,
                    weeks,
                    weeks
                )
            }

            difference < TimeUnit.DAYS.toMillis(365) -> {
                val months = (TimeUnit.MILLISECONDS.toDays(difference) / 30).toInt()
                context.resources.getQuantityString(
                    R.plurals.notifications_date_format_months,
                    months,
                    months
                )
            }

            else -> {
                ""
            }
        }
    }

    fun htmlContentToAnnotatedString(item: NotificationItem): AnnotatedString {
        val postTitle = item.contentContext.postTitle
        val modifiedHtml = if (postTitle.isNotEmpty()) {
            item.content.replace(postTitle, "\"${postTitle}\"")
        } else {
            item.content
        }

        val spanned = HtmlCompat.fromHtml(modifiedHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val builder = AnnotatedString.Builder()

        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            if (span is StyleSpan && span.style == Typeface.BOLD) {
                builder.addStyle(
                    style = SpanStyle(fontWeight = FontWeight.SemiBold),
                    start = start,
                    end = end
                )
            }
        }

        builder.append(spanned.trimEnd())
        return builder.toAnnotatedString()
    }
}
