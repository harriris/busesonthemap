package com.github.harriris.busesonthemap.util

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DatetimeFormatter(datetimeFormat: String) {
    private val localDateTimeFormat: DateFormat = DateFormat.getDateTimeInstance()
    private var busDateParser: SimpleDateFormat

    init {
        this.busDateParser = SimpleDateFormat(
            datetimeFormat,
            Locale.getDefault(),
        )
        this.busDateParser.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun format(timestamp: String?): String {
        if (timestamp == null || timestamp.isEmpty()) {
            return "Unknown timestamp"
        }
        val parsedDate: Date
        try {
            parsedDate = this.busDateParser.parse(timestamp) as Date
        } catch (exc: ParseException) {
            return "Unknown timestamp"
        }
        return this.localDateTimeFormat.format(parsedDate)
    }
}
