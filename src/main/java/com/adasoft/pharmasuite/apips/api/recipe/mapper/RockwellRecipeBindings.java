package com.adasoft.pharmasuite.apips.api.recipe.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.MeasuredValues;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.DateFormatUtil;
import com.datasweep.compatibility.client.AccessPrivilege;
import com.datasweep.compatibility.client.AccessPrivilegeFilter;
import com.datasweep.compatibility.client.DatasweepException;
import com.datasweep.compatibility.client.MasterRecipeFilter;
import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.PartFilter;
import com.datasweep.compatibility.client.ProcessBomFilter;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.datasweep.plantops.common.constants.filtering.IFilterComparisonOperators;
import com.datasweep.plantops.common.constants.filtering.IKeyedFilterAttributes;
import com.datasweep.plantops.common.constants.filtering.IMasterRecipeFilterAttributes;
import com.datasweep.plantops.common.constants.filtering.IProcessBomFilterAttributes;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.commons.base.ifc.services.ServiceFactory;
import com.rockwell.mes.services.s88.ifc.IS88RecipeService;
import com.rockwell.mes.services.s88.ifc.recipe.IMESProcedureFilter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Vector;

import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.CONTAINS;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.EQ;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.GE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.GT;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.LE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.LT;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.STARTSWITH;


/**
 * Enlace de campos OData de Workflow a ProcessOrderFilter/ProcessOrderItemFilter.
 * Mantiene la misma filosofía que RockwellBatchBindings, pero centrada en Workflows.
 */
public final class RockwellRecipeBindings {

    private RockwellRecipeBindings() {}

    private static Time toTime(String iso8601) {
        String iso = normalizeIso(iso8601);
        OffsetDateTime odt = OffsetDateTime.parse(iso);
        return DateFormatUtil.convertToSqlTime(odt.toString());
    }
    private static Time toTimePlus1ms(String iso8601) {
        String iso = normalizeIso(iso8601);
        OffsetDateTime odt = OffsetDateTime.parse(iso);
        return DateFormatUtil.convertToSqlTime(odt.plusNanos(1_000_000).toString()); // +1 ms
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

    private static final ThreadLocal<IMESProcedureFilter>   TL_PROC_FILTER = new ThreadLocal<>();
    private static final ThreadLocal<ProcessBomFilter>      TL_PBOM_FILTER = new ThreadLocal<>();
    private static final ThreadLocal<PartFilter>            TL_PART_FILTER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean>               TL_PROC_LINKED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean>               TL_PBOM_LINKED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean>               TL_PART_LINKED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean>               TL_MR_DIRTY = ThreadLocal.withInitial(() -> Boolean.FALSE);


    public static void finalizeAndClear() {
        // Limpieza de filtros intermedios
        TL_PBOM_FILTER.remove();
        TL_PBOM_LINKED.remove();

        TL_PART_FILTER.remove();
        TL_PART_LINKED.remove();

        TL_PROC_FILTER.remove();
        TL_PROC_LINKED.remove();

        // Limpieza del flag de "dirty"
        TL_MR_DIRTY.remove();
    }

    private static void markDirty() {
        TL_MR_DIRTY.set(Boolean.TRUE);
    }

    // ================================
    // Encadenado MasterRecipe -> Procedure (via FK "X_masterRecipe")
    // ================================
    private static IMESProcedureFilter getOrCreateProcedureFilter(MasterRecipeFilter masterRecipeFilter) throws DatasweepException {
        IMESProcedureFilter procedureFilter = TL_PROC_FILTER.get();
        if (procedureFilter == null) {
            IS88RecipeService s88 = ServiceFactory.getService(IS88RecipeService.class);
            procedureFilter = s88.createProcedureFilter();
            // Solo procedures de Master Recipe
            procedureFilter.forIsMasterRecipeEqualTo(Boolean.TRUE);
            TL_PROC_FILTER.set(procedureFilter);
        }

        if (Boolean.FALSE.equals(TL_PROC_LINKED.get())) {
            // Enlazamos MASTER_RECIPE (KEY) con X_Procedure.X_masterRecipe por subfiltro
            masterRecipeFilter.addSearchForSubFilter(
                    null,                                // sin nombre de atributo en el padre
                    IKeyedFilterAttributes.KEY,         // atributo del padre: master_recipe_key (2)
                    IFilterComparisonOperators.IN,      // operador de comparación: IN (obligatorio para SubFilters)
                    "X_masterRecipe",                   // columna FK en el hijo (Procedure)
                    (short) 7,                          // atributo de X_masterRecipe en el AT de Procedure
                    (com.datasweep.compatibility.client.Filter) procedureFilter
            );
            TL_PROC_LINKED.set(Boolean.TRUE);
            markDirty();
        }

        return procedureFilter;
    }

    /** Encadena MasterRecipe -> ProcessBom una sola vez por petición */
    private static ProcessBomFilter getOrCreateProcessBomFilter(IFunctionsEx fx, MasterRecipeFilter mr) {
        ProcessBomFilter pb = TL_PBOM_FILTER.get();
        if (pb == null) {
            pb = fx.createProcessBomFilter();
            TL_PBOM_FILTER.set(pb);
        }
        if (Boolean.FALSE.equals(TL_PBOM_LINKED.get())) {
            mr.addSearchForSubFilter(
                    IMasterRecipeFilterAttributes.BOM_KEY,
                    IFilterComparisonOperators.IN,
                    IKeyedFilterAttributes.KEY,
                    pb
            );
            TL_PBOM_LINKED.set(Boolean.TRUE);
            markDirty();
        }
        return pb;
    }

    /** Encadena ProcessBom -> Part una sola vez por petición */
    private static PartFilter getOrCreatePartFilter(IFunctionsEx fx, ProcessBomFilter pb) {
        PartFilter pf = TL_PART_FILTER.get();
        if (pf == null) {
            pf = fx.createPartFilter();
            TL_PART_FILTER.set(pf);
        }
        if (Boolean.FALSE.equals(TL_PART_LINKED.get())) {
            pb.addSearchForSubFilter(
                    IProcessBomFilterAttributes.PRODUCED_PART_KEY,  // padre: produced_part_key en PROCESS_BOM
                    IFilterComparisonOperators.IN,
                    IKeyedFilterAttributes.KEY,                     // hijo: part_key en PART
                    pf
            );
            TL_PART_LINKED.set(Boolean.TRUE);
            markDirty();
        }
        return pf;
    }
    /**
     * Registro de binds OData → MasterRecipeFilter.
     */
    public static FilterRegistry<MasterRecipeFilter> forRecipe(IFunctionsEx functionsEx) {
        var recipeFilterBuilder = new FilterRegistry.Builder<MasterRecipeFilter>();

         //name
        recipeFilterBuilder.bind("name", EnumSet.of(EQ, CONTAINS, STARTSWITH), (masterRecipeFilter, op, value) -> {
            switch (op) {
                case EQ         -> masterRecipeFilter.forNameEqualTo(value);
                case CONTAINS   -> masterRecipeFilter.forNameContaining(value);
                case STARTSWITH -> masterRecipeFilter.forNameStartingWith(value);
            }
            markDirty();
        });
        // Status (estado actual)
        recipeFilterBuilder.bind("status", EnumSet.of(EQ, CONTAINS, STARTSWITH), (filter, operation, value) -> {
            switch (operation) {
                case EQ        -> filter.forCurrentStateEqualTo(value);
                case CONTAINS  -> filter.forCurrentStateContaining(value);
                case STARTSWITH-> filter.forCurrentStateStartingWith(value);
            }
            markDirty();
        });

        // revision
        recipeFilterBuilder.bind("revision", EnumSet.of(EQ), (masterRecipeFilter, op, value) -> {
            switch (op) {
                case EQ         -> masterRecipeFilter.forRevisionEqual(value);
            }
            markDirty();
        });

        // creationDate
        recipeFilterBuilder.bind("creationDate", EnumSet.of(GE, GT, LE, LT, EQ), (masterRecipeFilter, op, iso) -> {
            Time t = toTime(iso);
            switch (op) {
                case GE -> masterRecipeFilter.forCreationTimeGreaterThanOrEqualTo(t);
                case GT -> masterRecipeFilter.forCreationTimeGreaterThanOrEqualTo(toTimePlus1ms(iso));
                case LT -> masterRecipeFilter.forCreationTimeLessThan(t);
                case LE -> masterRecipeFilter.forCreationTimeLessThan(toTimePlus1ms(iso));
                case EQ -> { masterRecipeFilter.forCreationTimeGreaterThanOrEqualTo(t); masterRecipeFilter.forCreationTimeLessThan(toTimePlus1ms(iso)); }
            }
            markDirty();
        });

        // effectivityStartTime
        recipeFilterBuilder.bind("effectivityStartTime", EnumSet.of(GE, GT, LE, LT, EQ), (masterRecipeFilter, op, iso) -> {
            Time t = toTime(iso); // aquí ya llevas fecha + hora

            short attr = IMasterRecipeFilterAttributes.EFFECTIVITY_START_TIME;

            switch (op) {
                case GE -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.GREATER_THAN_EQUAL_TO, t);

                case GT -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.GREATER_THAN, t);

                case LE -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.LESS_THAN_EQUAL_TO, t);

                case LT -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.LESS_THAN, t);

                case EQ -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.EQUAL_TO, t);
            }
            markDirty();
        });

        // effectivityEndTime
        recipeFilterBuilder.bind("effectivityEndTime", EnumSet.of(GE, GT, LE, LT, EQ), (masterRecipeFilter, op, iso) -> {
            Time t = toTime(iso);

            short attr = IMasterRecipeFilterAttributes.EFFECTIVITY_END_TIME;

            switch (op) {
                case GE -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.GREATER_THAN_EQUAL_TO, t);

                case GT -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.GREATER_THAN, t);

                case LE -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.LESS_THAN_EQUAL_TO, t);

                case LT -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.LESS_THAN, t);

                case EQ -> masterRecipeFilter.addSearchBy(
                        attr, IFilterComparisonOperators.EQUAL_TO, t);
            }
            markDirty();
        });

        // accessPrivilege (UDA)
        recipeFilterBuilder.bind("accessPrivilege", EnumSet.of(EQ, CONTAINS), (masterRecipeFilter, op, value) -> {

            // Si el filtro es nulo o vacío, no aplicamos ningún filtro (mostramos todos)
            if (value == null || value.isEmpty()) {
                return;
            }

            AccessPrivilegeFilter privilegeFilter = functionsEx.createAccessPrivilegeFilter();

            // EQ: nombre exacto; CONTAINS: nombre conteniendo el valor
            switch (op) {
                case EQ       -> privilegeFilter.forNameEqualTo(value);
                case CONTAINS -> privilegeFilter.forNameContaining(value);
                default       -> { /* no debería llegar, los ops ya están limitados */ }
            }

            Vector<?> vector = functionsEx.getFilteredAccessPrivileges(privilegeFilter);
            if (vector != null && !vector.isEmpty()) {
                for (Object item : vector) {
                    if (item instanceof AccessPrivilege accessPrivilege) {
                        masterRecipeFilter.forUdaEqualTo(UdaConstant.ACCESS_PRIVILEGE, accessPrivilege);
                    }
                }
            } else {
                // Si el privilegio no existe, lanzamos una excepción descriptiva
                throw new IllegalArgumentException("No data found with accessPrivilege '" + value + "'");
            }

            markDirty();
        });

        // materialName (encadena MaterialFilter una única vez por petición)
        recipeFilterBuilder.bind("MaterialName", EnumSet.of(EQ, CONTAINS, STARTSWITH), (masterRecipeFilter, op, value) -> {
            ProcessBomFilter processBomFilter = getOrCreateProcessBomFilter(functionsEx, masterRecipeFilter);
            PartFilter partFilter = getOrCreatePartFilter(functionsEx, processBomFilter);
            switch (op) {
                case EQ         -> partFilter.forPartNameEqualTo(value);
                case CONTAINS   -> partFilter.forPartNameContaining(value);
                case STARTSWITH -> partFilter.forPartNameStartingWith(value);
            }
            markDirty();
        });

        recipeFilterBuilder.bind("materialDescription", EnumSet.of(EQ, CONTAINS, STARTSWITH), (masterRecipeFilter, op, value) -> {
            ProcessBomFilter processBomFilter = getOrCreateProcessBomFilter(functionsEx, masterRecipeFilter);
            PartFilter partFilter = getOrCreatePartFilter(functionsEx, processBomFilter);

            switch (op) {
                case EQ         -> partFilter.forDescriptionEqualTo(value);
                case CONTAINS   -> partFilter.forDescriptionContaining(value);
                case STARTSWITH -> partFilter.forDescriptionStartingWith(value);
            }
            markDirty();
        });

        // ---------- Quantity (MeasuredValue) - retrocompatible ----------
        recipeFilterBuilder.bind("quantityWithUnitDisplay", EnumSet.of(GE, LE, EQ), (masterRecipeFilter, operation, raw) -> {
            MeasuredValue mv = toMeasured(masterRecipeFilter.getServer(),raw, ApiConstants.DEFAULT_QUANTITY_UOM);
            ProcessBomFilter processBomFilter = getOrCreateProcessBomFilter(functionsEx, masterRecipeFilter);
            PartFilter partFilter = getOrCreatePartFilter(functionsEx, processBomFilter);

            switch (operation) {
                case GE -> partFilter.forUdaGreaterThan(UdaConstant.PLANNED_QUANTITY,mv);
                case LE -> partFilter.forUdaLessThanOrEqualTo(UdaConstant.PLANNED_QUANTITY,mv);
                case EQ -> { partFilter.forUdaGreaterThanOrEqualTo(UdaConstant.PLANNED_QUANTITY,mv); partFilter.forUdaLessThanOrEqualTo(UdaConstant.PLANNED_QUANTITY,mv); }
            }
            markDirty();
        });

        // ---------- Quantity (value numérico separado) ----------
        recipeFilterBuilder.bind("quantity/value", EnumSet.of(GE, GT, LE, LT, EQ), (masterRecipeFilter, operation, raw) -> {
            MeasuredValue mv = toMeasured(masterRecipeFilter.getServer(), raw, ApiConstants.DEFAULT_QUANTITY_UOM);
            ProcessBomFilter processBomFilter = getOrCreateProcessBomFilter(functionsEx, masterRecipeFilter);
            PartFilter partFilter = getOrCreatePartFilter(functionsEx, processBomFilter);

            switch (operation) {
                case GE -> partFilter.forUdaGreaterThanOrEqualTo(UdaConstant.PLANNED_QUANTITY, mv);
                case GT -> partFilter.forUdaGreaterThan(UdaConstant.PLANNED_QUANTITY, mv);
                case LE -> partFilter.forUdaLessThanOrEqualTo(UdaConstant.PLANNED_QUANTITY, mv);
                case LT -> partFilter.forUdaLessThan(UdaConstant.PLANNED_QUANTITY, mv);
                case EQ -> { partFilter.forUdaGreaterThanOrEqualTo(UdaConstant.PLANNED_QUANTITY, mv); partFilter.forUdaLessThanOrEqualTo(UdaConstant.PLANNED_QUANTITY, mv); }
            }
            markDirty();
        });

        recipeFilterBuilder.bind("procedure", EnumSet.of(EQ, CONTAINS, STARTSWITH), (masterRecipeFilter, op, value) -> {
            IMESProcedureFilter procedureFilter = getOrCreateProcedureFilter(masterRecipeFilter);
            switch (op) {
                case EQ         -> procedureFilter.forProcedureNameEqualTo(value);
                case CONTAINS   -> procedureFilter.forProcedureNameContaining(value);
                case STARTSWITH -> procedureFilter.forProcedureNameStartingWith(value);
            }
            markDirty();
        });

        return recipeFilterBuilder.build();
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
