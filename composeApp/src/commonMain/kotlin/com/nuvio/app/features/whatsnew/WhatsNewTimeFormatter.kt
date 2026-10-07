package com.nuvio.app.features.whatsnew

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.periodUntil
import kotlin.time.Instant

internal enum class LastCheckedUnit {
    JUST_NOW,
    MINUTE,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

internal data class LastCheckedAge(
    val amount: Long,
    val unit: LastCheckedUnit,
)

internal fun calculateLastCheckedAge(
    fetchedAtMillis: Long,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): LastCheckedAge {
    val effectiveNowMillis = nowMillis.coerceAtLeast(fetchedAtMillis)
    val elapsedMillis = effectiveNowMillis - fetchedAtMillis

    val minuteMillis = 60_000L
    val hourMillis = 60L * minuteMillis
    val dayMillis = 24L * hourMillis

    if (elapsedMillis < minuteMillis) {
        return LastCheckedAge(amount = 0L, unit = LastCheckedUnit.JUST_NOW)
    }

    if (elapsedMillis < hourMillis) {
        return LastCheckedAge(
            amount = elapsedMillis / minuteMillis,
            unit = LastCheckedUnit.MINUTE,
        )
    }

    val start = Instant.fromEpochMilliseconds(fetchedAtMillis)
    val end = Instant.fromEpochMilliseconds(effectiveNowMillis)
    val calendarPeriod = start.periodUntil(end, timeZone)
    // Jan 31 → Feb 28 is a whole calendar month even though "Feb 31" doesn't exist, which
    // periodUntil reports as 0 months and 28 days.
    val months = calendarPeriod.months + if (calendarPeriod.months == 0 && endsAMonthEarly(start, end, timeZone)) 1 else 0

    return when {
        calendarPeriod.years >= 1 -> LastCheckedAge(
            amount = calendarPeriod.years.toLong(),
            unit = LastCheckedUnit.YEAR,
        )
        months >= 1 -> LastCheckedAge(
            amount = months.toLong(),
            unit = LastCheckedUnit.MONTH,
        )
        calendarPeriod.days >= 7 -> LastCheckedAge(
            amount = (calendarPeriod.days / 7L),
            unit = LastCheckedUnit.WEEK,
        )
        calendarPeriod.days >= 1 -> LastCheckedAge(
            amount = calendarPeriod.days.toLong(),
            unit = LastCheckedUnit.DAY,
        )
        elapsedMillis >= dayMillis -> LastCheckedAge(
            amount = elapsedMillis / dayMillis,
            unit = LastCheckedUnit.DAY,
        )
        else -> LastCheckedAge(
            amount = elapsedMillis / hourMillis,
            unit = LastCheckedUnit.HOUR,
        )
    }
}

/** [end] falls on the last day of its month, the month after [start]'s, on an earlier day number. */
private fun endsAMonthEarly(start: Instant, end: Instant, timeZone: TimeZone): Boolean {
    val startDate = start.toLocalDateTime(timeZone).date
    val endDate = end.toLocalDateTime(timeZone).date
    val nextMonth = startDate.plus(1, DateTimeUnit.MONTH)
    return endDate.year == nextMonth.year &&
        endDate.month == nextMonth.month &&
        endDate.day < startDate.day &&
        endDate.plus(1, DateTimeUnit.DAY).month != endDate.month
}
