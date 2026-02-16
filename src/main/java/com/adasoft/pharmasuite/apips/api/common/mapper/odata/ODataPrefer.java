package com.adasoft.pharmasuite.apips.api.common.mapper.odata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ODataPrefer {
    private static final Pattern MAX_PAGE = Pattern.compile("(?i)odata\\.maxpagesize\\s*=\\s*(\\d+)");
    private ODataPrefer(){}

    /** Devuelve N si viene "Prefer: odata.maxpagesize=N"; si no, null. */
    public static Integer resolveMaxPageSize(String preferHeader) {
        if (preferHeader == null) return null;
        Matcher m = MAX_PAGE.matcher(preferHeader);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
