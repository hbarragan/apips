package com.adasoft.pharmasuite.apips.core.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class LogManagement {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS z");

    public static void info(String str, Object className) {
        System.out.println(ZonedDateTime.now().format(formatter) + " [apips] INFO " + className.getClass().getName() + ": ### " + str);
    }

    public static void error(String str, Object classname) {
        System.err.println(ZonedDateTime.now().format(formatter) + " [apips] ERROR " + classname.getClass().getName() + ": ### " + str);
    }
}
