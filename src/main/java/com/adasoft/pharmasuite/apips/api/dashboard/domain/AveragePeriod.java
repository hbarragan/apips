package com.adasoft.pharmasuite.apips.api.dashboard.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AveragePeriod {
    private final int years;
    private final int months;
    private final int days;
    private final int hours;
    private final int minutes;
    private final int seconds;
    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        if (years != 0)   parts.add(years   + years>1? years + " años": years + " año");
        if (months != 0)  parts.add(months  + months>1? months + " meses": months + " mes");
        if (days != 0)    parts.add(days    + days>1? days + " días": days + " día");
        if (hours != 0)   parts.add(hours   + hours>1? hours + " horas": hours + " hora");
        if (minutes != 0) parts.add(minutes + minutes>1? minutes + " minutos": minutes + " minuto");
        if (seconds != 0) parts.add(seconds + seconds>1? seconds + " segundos": seconds + " segundo");
        if (parts.isEmpty()) {
            return "0 segundos";
        }
        return String.join(", ", parts);
    }
}
