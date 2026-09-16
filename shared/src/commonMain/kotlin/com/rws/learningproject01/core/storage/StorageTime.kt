package com.rws.learningproject01.core.storage

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal object StorageTime {
    fun nowIso(): String {
        val instant = Clock.System.now()
        val local = instant.toLocalDateTime(TimeZone.UTC)
        val millis = instant.toEpochMilliseconds() % 1000
        return buildString {
            append(local.year.toString().padStart(4, '0'))
            append('-')
            append(local.monthNumber.toString().padStart(2, '0'))
            append('-')
            append(local.dayOfMonth.toString().padStart(2, '0'))
            append('T')
            append(local.hour.toString().padStart(2, '0'))
            append(':')
            append(local.minute.toString().padStart(2, '0'))
            append(':')
            append(local.second.toString().padStart(2, '0'))
            append('.')
            append(millis.toString().padStart(3, '0'))
            append('Z')
        }
    }
}
