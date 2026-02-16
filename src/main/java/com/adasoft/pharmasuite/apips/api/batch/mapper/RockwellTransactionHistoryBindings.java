package com.adasoft.pharmasuite.apips.api.batch.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry;
import com.rockwell.mes.services.inventory.ifc.TransactionHistoryObject;
import com.rockwell.mes.services.inventory.ifc.TransactionSubtype;
import com.rockwell.mes.services.inventory.ifc.TransactionType;
import com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;

import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.EQ;

public final class RockwellTransactionHistoryBindings {

    private RockwellTransactionHistoryBindings() {}

    public static FilterRegistry<TransactionHistoryFilter> forTransactionHistory() {
        var filterBuilder = new FilterRegistry.Builder<TransactionHistoryFilter>()
                .caseInsensitiveKeys();

        // BatchName
        filterBuilder.bind("batchIdNew", EnumSet.of(EQ),
                (filter, operation, value) -> {
                    if (operation == EQ) {
                        filter.forBatchIdentifierNewEqualTo(value);
                    }
                });

        // orderAssociateType -> IN (X_transactionType)
        filterBuilder.bind("type", EnumSet.of(EQ),
                (filter, operation, raw) -> {
                    if (operation != EQ) return;

                    long[] typeValues = parseTransactionTypes(raw);
                    if (typeValues.length > 0) {
                        filter.forColumnNameIn(
                                TransactionHistoryObject.COL_NAME_TRANSACTION_TYPE,
                                typeValues
                        );
                    }
                });

        // orderAssociateSubtype -> IN (X_transactionSubtype)
        filterBuilder.bind("subtype", EnumSet.of(EQ),
                (filter, operation, raw) -> {
                    if (operation != EQ) return;

                    long[] subtypeValues = parseTransactionSubtypes(raw);
                    if (subtypeValues.length > 0) {
                        filter.forColumnNameIn(
                                TransactionHistoryObject.COL_NAME_TRANSACTION_SUBTYPE,
                                subtypeValues
                        );
                    }
                });



        return filterBuilder.build();
    }

    /**
     * Permite multi-valor en un único EQ separando por coma/; / | :
     *   orderAssociateType eq 'INPUT,OUTPUT'
     *   orderAssociateType eq 'INPUT|OUTPUT'
     */
    private static String[] splitMulti(String raw) {
        if (raw == null) return new String[0];
        return Arrays.stream(raw.split("[,;|]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private static long[] parseTransactionTypes(String raw) {
        return Arrays.stream(splitMulti(raw))
                .map(s -> TransactionType.valueOf(s.toUpperCase(Locale.ROOT)))
                .mapToLong(TransactionType::getValue)
                .distinct()
                .toArray();
    }

    private static long[] parseTransactionSubtypes(String raw) {
        return Arrays.stream(splitMulti(raw))
                .map(s -> TransactionSubtype.valueOf(s.toUpperCase(Locale.ROOT)))
                .mapToLong(TransactionSubtype::getValue)
                .distinct()
                .toArray();
    }
}