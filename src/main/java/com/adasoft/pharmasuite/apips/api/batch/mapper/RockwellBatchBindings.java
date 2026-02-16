package com.adasoft.pharmasuite.apips.api.batch.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.MeasuredValues;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.DateFormatUtil;
import com.datasweep.compatibility.client.BatchFilter;
import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.PartFilter;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;

import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.CONTAINS;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.EQ;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.GE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.GT;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.LE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.LT;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.STARTSWITH;

public final class RockwellBatchBindings {

    private RockwellBatchBindings() {}

    private static Time toTime(String iso8601) {
        String iso = normalizeIso(iso8601);
        OffsetDateTime odt = OffsetDateTime.parse(iso);
        return DateFormatUtil.convertToSqlTime(odt.toString()); // ya lo usáis en applyFilter
    }
    private static Time toTimePlus1ms(String iso8601) {
        String iso = normalizeIso(iso8601);
        OffsetDateTime odt = OffsetDateTime.parse(iso);
        return DateFormatUtil.convertToSqlTime(odt.plusNanos(1_000_000).toString()); // +1 ms
    }

    public static FilterRegistry<BatchFilter> forBatch(IFunctionsEx fx,
                                                       String defaultQtyUom,
                                                       String defaultPotencyUom) {
        var batchFilterBuilder = new FilterRegistry.Builder<BatchFilter>().caseInsensitiveKeys();
        String qtyUom = defaultQtyUom == null ? "" : defaultQtyUom;
        String potUom = defaultPotencyUom == null ? "" : defaultPotencyUom;

        // Name
        batchFilterBuilder.bind("name", EnumSet.of(EQ, CONTAINS, STARTSWITH),
                (filter, operation, value) -> {
                    switch (operation) {
                        case EQ        -> filter.forNameEqualTo(value);
                        case CONTAINS  -> filter.forNameContaining(value);
                        case STARTSWITH-> filter.forNameStartingWith(value);
                    }
                });

        // Status (estado actual)
        batchFilterBuilder.bind("status", EnumSet.of(EQ, CONTAINS, STARTSWITH),
                (filter, operation, value) -> {
                    switch (operation) {
                        case EQ        -> filter.forCurrentStateEqualTo(value);
                        case CONTAINS  -> filter.forCurrentStateContaining(value);
                        case STARTSWITH-> filter.forCurrentStateStartingWith(value);
                    }
                });

        // CreationTime
        batchFilterBuilder.bind("creationtime", EnumSet.of(GE, GT, LE, LT, EQ),
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

        // ExpiryDate → ExpirationTime
        batchFilterBuilder.bind("expirydate", EnumSet.of(GE, GT, LE, LT, EQ),
                (filter, operation, iso) -> {
                    Time time = toTime(iso);
                    switch (operation) {
                        case GE -> filter.forExpirationTimeGreaterThanOrEqualTo(time);
                        case GT -> filter.forExpirationTimeGreaterThanOrEqualTo(toTimePlus1ms(iso));
                        case LT -> filter.forExpirationTimeLessThan(time);
                        case LE -> filter.forExpirationTimeLessThan(toTimePlus1ms(iso));
                        case EQ -> { filter.forExpirationTimeGreaterThanOrEqualTo(time);
                            filter.forExpirationTimeLessThan(toTimePlus1ms(iso)); }
                    }
                });


        batchFilterBuilder.bind("materialName", EnumSet.of(EQ, CONTAINS, STARTSWITH), (filter, operation, value) -> {
            PartFilter partFilter = fx.createPartFilter(); // <- creado con el mismo Server interno
            switch (operation) {
                case EQ         -> partFilter.forPartNameEqualTo(value);
                case CONTAINS   -> partFilter.forPartNameContaining(value);
                case STARTSWITH -> partFilter.forPartNameStartingWith(value);
            }
            filter.forPartsIn(partFilter);
        });

        // ---------- MaterialDescription ----------
        batchFilterBuilder.bind("materialDescription", EnumSet.of(EQ, CONTAINS, STARTSWITH), (filter, operation, value) -> {
            PartFilter partFilter = fx.createPartFilter();
            switch (operation) {
                case EQ         -> partFilter.forDescriptionEqualTo(value);
                case CONTAINS   -> partFilter.forDescriptionContaining(value);
                case STARTSWITH -> partFilter.forDescriptionStartingWith(value);
            }
            filter.forPartsIn(partFilter);
        });

        // ---------- Quantity (MeasuredValue) - retrocompatible ----------
        batchFilterBuilder.bind("quantityWithUnitDisplay", EnumSet.of(GE, LE, EQ), (filter, operation, raw) -> {
            MeasuredValue mv = toMeasured(filter.getServer(),raw, qtyUom);
            switch (operation) {
                case GE -> filter.forQuantityGreaterThanOrEqualTo(mv);
                case LE -> filter.forQuantityLessThanOrEqualTo(mv);
                case EQ -> { filter.forQuantityGreaterThanOrEqualTo(mv); filter.forQuantityLessThanOrEqualTo(mv); }
            }
        });

        // ---------- Quantity (value numérico separado) ----------
        batchFilterBuilder.bind("quantity/value", EnumSet.of(GE, GT, LE, LT, EQ), (filter, operation, raw) -> {
            MeasuredValue mv = toMeasured(filter.getServer(),raw, qtyUom);
            switch (operation) {
                case GE -> filter.forQuantityGreaterThanOrEqualTo(mv);
                case GT -> filter.forQuantityGreaterThan(mv);
                case LE -> filter.forQuantityLessThanOrEqualTo(mv);
                case LT -> filter.forQuantityLessThan(mv);
                case EQ -> { filter.forQuantityGreaterThanOrEqualTo(mv); filter.forQuantityLessThanOrEqualTo(mv); }
            }
        });

        // ---------- Potency (MeasuredValue) ----------
        batchFilterBuilder.bind("potencyWithUnitDisplay", EnumSet.of(GE, LE, EQ), (filter, operation, raw) -> {
            MeasuredValue mv = toMeasured(filter.getServer(),raw, potUom);
            switch (operation) {
                case GE -> filter.forPotencyGreaterThanOrEqualTo(mv);
                case LE -> filter.forPotencyLessThanOrEqualTo(mv);
                case EQ -> { filter.forPotencyGreaterThanOrEqualTo(mv); filter.forPotencyLessThanOrEqualTo(mv); }
            }
        });


        batchFilterBuilder.bind("retestdate", EnumSet.of(GE, GT, LE, LT, EQ), (filter, operation, iso) -> {
            Time time = toTime(iso);
            String key = UdaConstant.RETEST_DATE;
            switch (operation) {
                case GE -> filter.forUdaGreaterThanOrEqualTo(key, time);
                case LE -> filter.forUdaLessThan(key, time);
                case EQ -> { filter.forUdaGreaterThanOrEqualTo(key, time); filter.forUdaLessThan(key, toTimePlus1ms(iso)); }
                case GT -> filter.forUdaGreaterThanOrEqualTo(key, toTimePlus1ms(iso));
                case LT -> filter.forUdaLessThan(key,time);
            }
        });



        batchFilterBuilder.bind("productiondate", EnumSet.of(GE, GT, LE, LT, EQ), (filter, operation, iso) -> {
            Time time = toTime(iso);
            String key = UdaConstant.PRODUCTION_DATE;
            switch (operation) {
                case GE -> filter.forUdaGreaterThanOrEqualTo(key, time);
                case LE -> filter.forUdaLessThan(key, time);
                case EQ -> { filter.forUdaGreaterThanOrEqualTo(key, time); filter.forUdaLessThan(key, toTimePlus1ms(iso)); }
                case GT -> filter.forUdaGreaterThanOrEqualTo(key, toTimePlus1ms(iso));
                case LT -> filter.forUdaLessThan(key,time);
            }
        });
        return batchFilterBuilder.build();
    }

    private static String normalizeIso(String raw) {
        String s = raw.trim();

        // Si viene con comillas de OData tipo '2025-10-20T00:00:00Z'
        if (s.startsWith("'") && s.endsWith("'") && s.length() > 1) {
            s = s.substring(1, s.length() - 1);
        }

        // Por si en algún caso viene "2025-10-20 00:00:00Z"
        s = s.replace(" ", "T");
        return s;
    }

    private static MeasuredValue toMeasured(ServerImpl server, String raw, String defaultUom) throws Exception {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("MeasuredValue vacío");
        }
        String valueRaw = raw.trim();
        String uom = defaultUom;

        // normaliza separadores "100|kg" → "100 kg"
        valueRaw = valueRaw.replace('|', ' ');

        // si viene junto tipo "100kg"
        int splitAt = -1;
        for (int i = 0; i < valueRaw.length(); i++) {
            char c = valueRaw.charAt(i);
            if (!(Character.isDigit(c) || c == '.' || c == ',')) {
                splitAt = i;
                break;
            }
        }
        String numberPart;
        if (splitAt >= 0) {
            numberPart = valueRaw.substring(0, splitAt).trim();
            String after = valueRaw.substring(splitAt).trim();
            if (!after.isEmpty()) uom = after;
        } else {
            // o venía "100 kg" ya separado
            String[] toks = valueRaw.split("\\s+", 2);
            numberPart = toks[0].trim();
            if (toks.length > 1 && !toks[1].isBlank()) uom = toks[1].trim();
        }

        numberPart = numberPart.replace(',', '.'); // seguridad locales
        BigDecimal value = new BigDecimal(numberPart);

        // IMPORTANTE: si vuestro JAR tiene otra signatura del ctor, ajustad aquí.
        return MeasuredValues.of(server, value, uom);
    }
}