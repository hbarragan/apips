package com.adasoft.pharmasuite.apips.api.common.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ODataPaging {

    private final Integer pageSize;
    private final Integer offset;
    private final boolean countRequested;

    public static ODataPaging from(Integer top, Integer skip, Boolean countReq,
                                   Integer preferMaxPageSize,
                                   Integer defaultTop, Integer maxTop) {

        Integer size = (top != null) ? top : (preferMaxPageSize);
        size = clamp(size==null?0:size, 1, maxTop);
        int off = (skip != null) ? Math.max(0, skip) : 0;
        return new ODataPaging(size, off, Boolean.TRUE.equals(countReq));
    }

    private static int clamp(Integer v, Integer min, Integer max) {
        return Math.max(min, Math.min(v, max));
    }
}
