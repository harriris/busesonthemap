package com.github.harriris.busesonthemap.util

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DatetimeFormatter(datetimeFormat: String) {
    private var localDateTimeFormat: DateFormat = DateFormat.getDateTimeInstance()
    private var busDateParser: SimpleDateFormat

    init {
        busDateParser = SimpleDateFormat(
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
            parsedDate = busDateParser.parse(timestamp) as Date
        } catch (exc: ParseException) {
            return "Unknown timestamp"
        }
        return localDateTimeFormat.format(parsedDate)
    }
}
