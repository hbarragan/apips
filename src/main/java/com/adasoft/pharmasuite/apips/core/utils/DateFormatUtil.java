package com.adasoft.pharmasuite.apips.core.utils;

import com.datasweep.compatibility.ui.Time;

import java.time.OffsetDateTime;
import java.util.Calendar;

public class DateFormatUtil {

    public static Time convertToSqlTime(String zonedDateTime) {
        // Reemplazar el espacio por 'T' para cumplir con el formato ISO 8601
        String fechaFormateada = zonedDateTime.replace(" ", "T");
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(fechaFormateada);
        long epochMillis = offsetDateTime.toInstant().toEpochMilli();
        long wfcTime = (epochMillis + 59011459200000L) * 10000L;
        return new Time(wfcTime);
    }

    public static Calendar truncateToDay(Calendar cal) {
        if (cal == null) {
            return null;
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

}
