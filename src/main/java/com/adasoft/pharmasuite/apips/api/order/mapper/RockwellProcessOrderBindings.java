package com.adasoft.pharmasuite.apips.api.order.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.DateFormatUtil;
import com.datasweep.compatibility.client.ProcessOrderFilter;
import com.datasweep.compatibility.client.ProcessOrderItemFilter;
import com.datasweep.compatibility.ui.Time;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;

import java.time.OffsetDateTime;
import java.util.EnumSet;

import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.CONTAINS;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.EQ;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.GE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.GT;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.LE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.LT;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.STARTSWITH;

public final class RockwellProcessOrderBindings {

    private RockwellProcessOrderBindings() {}

    private static Time toTime(String iso8601) {
        String iso = normalizeIso(iso8601);
        OffsetDateTime odt = OffsetDateTime.parse(iso);
        return DateFormatUtil.convertToSqlTime(odt.toString());
    }

    private static Time toTimePlus1ms(String iso8601) {
        String iso = normalizeIso(iso8601);
        OffsetDateTime odt = OffsetDateTime.parse(iso);
        return DateFormatUtil.convertToSqlTime(odt.plusNanos(1_000_000).toString());
    }

    public static FilterRegistry<ProcessOrderFilter> forProcessOrder(IFunctionsEx fx) {
        var builder = new FilterRegistry.Builder<ProcessOrderFilter>().caseInsensitiveKeys();

        builder.bind("name", EnumSet.of(EQ, CONTAINS, STARTSWITH),
                (filter, operation, value) -> {
                    switch (operation) {
                        case EQ -> filter.forNameEqualTo(value);
                        case CONTAINS -> filter.forNameContaining(value);
                        case STARTSWITH -> filter.forNameStartingWith(value);
                    }
                });

        builder.bind("creationdate", EnumSet.of(GE, GT, LE, LT, EQ),
                (filter, operation, iso) -> {
                    Time time = toTime(iso);
                    switch (operation) {
                        case GE -> filter.forCreationTimeGreaterThanOrEqualTo(time);
                        case GT -> filter.forCreationTimeGreaterThanOrEqualTo(toTimePlus1ms(iso));
                        case LT -> filter.forCreationTimeLessThan(time);
                        case LE -> filter.forCreationTimeLessThan(toTimePlus1ms(iso));
                        case EQ -> {
                            filter.forCreationTimeGreaterThanOrEqualTo(time);
                            filter.forCreationTimeLessThan(toTimePlus1ms(iso));
                        }
                    }
                });

        return builder.build();
    }

    public static FilterRegistry<ProcessOrderItemFilter> forProcessOrderItem(IFunctionsEx fx) {
        var builder = new FilterRegistry.Builder<ProcessOrderItemFilter>().caseInsensitiveKeys();

        builder.bind("status", EnumSet.of(EQ, CONTAINS, STARTSWITH),
                (filter, operation, value) -> {
                    switch (operation) {
                        case EQ -> filter.forCurrentStateEqualTo(value);
                        case CONTAINS -> filter.forCurrentStateContaining(value);
                        case STARTSWITH -> filter.forCurrentStateStartingWith(value);
                    }
                });

        builder.bind("erpstartdate", EnumSet.of(GE, GT, LE, LT, EQ),
                (filter, operation, iso) -> {
                    Time time = toTime(iso);
                    switch (operation) {
                        case GE -> filter.forUdaGreaterThanOrEqualTo(UdaConstant.ERP_START_DATE, time);
                        case GT -> filter.forUdaGreaterThanOrEqualTo(UdaConstant.ERP_START_DATE, toTimePlus1ms(iso));
                        case LT -> filter.forUdaLessThan(UdaConstant.ERP_START_DATE, time);
                        case LE -> filter.forUdaLessThan(UdaConstant.ERP_START_DATE, toTimePlus1ms(iso));
                        case EQ -> {
                            filter.forUdaGreaterThanOrEqualTo(UdaConstant.ERP_START_DATE, time);
                            filter.forUdaLessThan(UdaConstant.ERP_START_DATE, toTimePlus1ms(iso));
                        }
                    }
                });

        builder.bind("erpfinishdate", EnumSet.of(GE, GT, LE, LT, EQ),
                (filter, operation, iso) -> {
                    Time time = toTime(iso);
                    switch (operation) {
                        case GE -> filter.forUdaGreaterThanOrEqualTo(UdaConstant.ERP_FINISH_DATE, time);
                        case GT -> filter.forUdaGreaterThanOrEqualTo(UdaConstant.ERP_FINISH_DATE, toTimePlus1ms(iso));
                        case LT -> filter.forUdaLessThan(UdaConstant.ERP_FINISH_DATE, time);
                        case LE -> filter.forUdaLessThan(UdaConstant.ERP_FINISH_DATE, toTimePlus1ms(iso));
                        case EQ -> {
                            filter.forUdaGreaterThanOrEqualTo(UdaConstant.ERP_FINISH_DATE, time);
                            filter.forUdaLessThan(UdaConstant.ERP_FINISH_DATE, toTimePlus1ms(iso));
                        }
                    }
                });

        builder.bind("accessprivilege", EnumSet.of(EQ, CONTAINS),
                (filter, operation, value) -> {
                    switch (operation) {
                        case EQ -> filter.forUdaEqualTo(UdaConstant.ACCESS_PRIVILEGE, value);
                        case CONTAINS -> filter.forUdaContaining(UdaConstant.ACCESS_PRIVILEGE, value);
                    }
                });

        return builder.build();
    }

    private static String normalizeIso(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("ISO date cannot be null or blank");
        }
        String trimmed = raw.trim();
        if (trimmed.endsWith("Z")) {
            return trimmed;
        }
        if (trimmed.matches(".*[+-]\\d{2}:\\d{2}$")) {
            return trimmed;
        }
        return trimmed + "Z";
    }
}
