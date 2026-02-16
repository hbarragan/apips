package com.adasoft.pharmasuite.apips.api.workflow.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.MeasuredValues;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.DateFormatUtil;
import com.datasweep.compatibility.client.AccessPrivilegeFilter;
import com.datasweep.compatibility.client.ControlRecipeFilter;
import com.datasweep.compatibility.client.MasterRecipeFilter;
import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.ProcessOrderFilter;
import com.datasweep.compatibility.client.ProcessOrderItemFilter;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.datasweep.plantops.common.constants.filtering.IControlRecipeFilterAttributes;
import com.datasweep.plantops.common.constants.filtering.IFilterComparisonOperators;
import com.datasweep.plantops.common.constants.filtering.IKeyedFilterAttributes;
import com.datasweep.plantops.common.constants.filtering.IProcessOrderItemFilterAttributes;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.services.s88.ifc.S88ProcessingType;

import java.math.BigDecimal;
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
public final class RockwellWorkflowBindings {


    private RockwellWorkflowBindings() {}


    private static Time toTime(String iso8601) { return DateFormatUtil.convertToSqlTime(iso8601); }
    private static Time toTimePlus1ms(String iso8601) {
        var odt = java.time.OffsetDateTime.parse(iso8601);
        return DateFormatUtil.convertToSqlTime(odt.plusNanos(1_000_000).toString());
    }

    // ==================================================
    //  Acumulador Thread-Local de subfiltro de "items"
    // ==================================================

    /**
     * hilos/petición donde se acumulan las restricciones.
     */
    private static final ThreadLocal<ProcessOrderItemFilter>    TL_ITEM_FILTER  = new ThreadLocal<>();
    private static final ThreadLocal<ControlRecipeFilter>       TL_CR_FILTER    = new ThreadLocal<>();
    private static final ThreadLocal<MasterRecipeFilter>        TL_MR_FILTER    = new ThreadLocal<>();
    private static final ThreadLocal<Boolean>                   TL_CRMR_LINKED  = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean>                   TL_ITEM_DIRTY   = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean>                   TL_BASELINE_APPLIED = ThreadLocal.withInitial(() -> Boolean.FALSE);


    private static ProcessOrderItemFilter getOrCreateItemFilter(IFunctionsEx functionsEx) {
        ProcessOrderItemFilter processOrderItemFilter = TL_ITEM_FILTER.get();
        if (processOrderItemFilter == null) {
            processOrderItemFilter = functionsEx.createProcessOrderItemFilter();
            processOrderItemFilter.forUdaEqualTo(
                    UdaConstant.PROCESSING_TYPE,
                    S88ProcessingType.WORKFLOW.getChoiceElement().getValue()
            );
            TL_ITEM_FILTER.set(processOrderItemFilter);
        }
        return processOrderItemFilter;
    }

    private static void markDirty() {
        TL_ITEM_DIRTY.set(Boolean.TRUE);
    }

    private static void clearAccumulatedItemFilter() {
        TL_ITEM_FILTER.remove();
        TL_ITEM_DIRTY.remove();
        TL_CR_FILTER.remove();
        TL_MR_FILTER.remove();
        TL_CRMR_LINKED.remove();
        TL_BASELINE_APPLIED.remove();
    }

    private static void ensureBaselineItemFilter(IFunctionsEx fx) {
        if (Boolean.TRUE.equals(TL_BASELINE_APPLIED.get())) return;

        ProcessOrderItemFilter item = TL_ITEM_FILTER.get();
        if (item == null) {
            item = fx.createProcessOrderItemFilter();
            TL_ITEM_FILTER.set(item);
        }

        // Filtro fijo de tipo de procesamiento a workflows
        item.forUdaEqualTo(
                UdaConstant.PROCESSING_TYPE,
                S88ProcessingType.WORKFLOW.getChoiceElement().getValue()
        );

        TL_BASELINE_APPLIED.set(Boolean.TRUE);
        markDirty(); // fuerza que se adjunte en attachAccumulatedItemFilter()
    }

    /**
     * Debes llamar a este método **después** de haber aplicado todos los binds OData
     * sobre el ProcessOrderFilter (padre). Si se acumularon restricciones de item,
     * se añade el subfiltro con addSearchForSubFilter(...).
     */
    public static void attachAccumulatedItemFilter(ProcessOrderFilter parent) {
        try {
            Boolean dirty = TL_ITEM_DIRTY.get();
            ProcessOrderItemFilter item = TL_ITEM_FILTER.get();
            if (Boolean.TRUE.equals(dirty) && item != null) {
                parent.addSearchForSubFilter(
                        IKeyedFilterAttributes.KEY,
                        IFilterComparisonOperators.IN,
                        IProcessOrderItemFilterAttributes.ORDER_KEY,
                        item
                );
            }
        } finally {
            // Limpia siempre el estado del hilo.
            clearAccumulatedItemFilter();
        }
    }

    /**
     * Devuelve un MasterRecipeFilter listo para usar y evita duplicar addSearchForSubFilter
     * creando y enlazando la cadena Item -> ControlRecipe -> MasterRecipe solo una vez.
     */
    private static MasterRecipeFilter getOrCreateMasterRecipeFilter(IFunctionsEx fx) {
        // Asegura que existe el itemFilter de la petición
        ProcessOrderItemFilter processOrderItemFilter = getOrCreateItemFilter(fx);

        MasterRecipeFilter masterRecipeFilter = TL_MR_FILTER.get();
        if (masterRecipeFilter == null) {
            ControlRecipeFilter controlRecipeFilter = fx.createControlRecipeFilter();
            masterRecipeFilter = fx.createMasterRecipeFilter();

            TL_CR_FILTER.set(controlRecipeFilter);
            TL_MR_FILTER.set(masterRecipeFilter);
            TL_CRMR_LINKED.set(Boolean.FALSE);
        }

        // Enlazar CR -> MR e Item -> CR solo una vez
        if (Boolean.FALSE.equals(TL_CRMR_LINKED.get())) {
            ControlRecipeFilter cr = TL_CR_FILTER.get();

            // ControlRecipe -> MasterRecipe (por clave de MasterRecipe)
            cr.addSearchForSubFilter(
                    IControlRecipeFilterAttributes.MASTERRECIPE_KEY,
                    IFilterComparisonOperators.IN,
                    IKeyedFilterAttributes.KEY,
                    masterRecipeFilter
            );

            // Item -> ControlRecipe (por clave de ProcessOrderItem)
            processOrderItemFilter.addSearchForSubFilter(
                    IKeyedFilterAttributes.KEY,
                    IFilterComparisonOperators.IN,
                    IControlRecipeFilterAttributes.PROCESSORDERITEM_KEY,
                    cr
            );

            TL_CRMR_LINKED.set(Boolean.TRUE);
            TL_ITEM_DIRTY.set(Boolean.TRUE);
        }

        return masterRecipeFilter;
    }


    public static FilterRegistry<ProcessOrderFilter> forWorkflow(IFunctionsEx functionsEx) {
        var processOrderFilterBuilder = new FilterRegistry.Builder<ProcessOrderFilter>();

        ensureBaselineItemFilter(functionsEx);

// name
        processOrderFilterBuilder.bind("name", EnumSet.of(EQ, CONTAINS, STARTSWITH), (filter, op, value) -> {
            switch (op) {
                case EQ -> filter.forNameEqualTo(value);
                case CONTAINS -> filter.forNameContaining(value);
                case STARTSWITH -> filter.forNameStartingWith(value);
            }
        });

// description
        processOrderFilterBuilder.bind("description", EnumSet.of(EQ, CONTAINS, STARTSWITH), (filter, op, value) -> {
            switch (op) {
                case EQ -> filter.forDescriptionEqualTo(value);
                case CONTAINS -> filter.forDescriptionContaining(value);
                case STARTSWITH -> filter.forDescriptionStartingWith(value);
            }
        });


// creationDate (ProcessOrderFilter)
        processOrderFilterBuilder.bind("creationDate", EnumSet.of(GE, GT, LE, LT, EQ), (filter, op, iso) -> {
            Time t = toTime(iso);
            switch (op) {
                case GE -> filter.forCreationTimeGreaterThanOrEqualTo(t);
                case GT -> filter.forCreationTimeGreaterThanOrEqualTo(toTimePlus1ms(iso));
                case LT -> filter.forCreationTimeLessThan(t);
                case LE -> filter.forCreationTimeLessThan(toTimePlus1ms(iso));
                case EQ -> { filter.forCreationTimeGreaterThanOrEqualTo(t); filter.forCreationTimeLessThan(toTimePlus1ms(iso)); }
            }
        });

        processOrderFilterBuilder.bind("actualStart", EnumSet.of(GE, GT, LE, LT, EQ), (filter, op, iso) -> {
            Time t = toTime(iso);
            ProcessOrderItemFilter item = getOrCreateItemFilter(functionsEx);

            switch (op) {
                case GE -> {
                    item.forUdaGreaterThanOrEqualTo(UdaConstant.ACTUAL_START_DATE,t);
                    markDirty();
                }
                case GT -> {
                    item.forUdaGreaterThanOrEqualTo(UdaConstant.ACTUAL_START_DATE,toTimePlus1ms(iso));
                    markDirty();
                }
                case LT -> {
                    item.forUdaLessThan(UdaConstant.ACTUAL_START_DATE,t);
                    markDirty();
                }
                case LE -> {
                    item.forUdaLessThan(UdaConstant.ACTUAL_START_DATE,toTimePlus1ms(iso));
                    markDirty();
                }
                case EQ -> {
                    item.forUdaGreaterThanOrEqualTo(UdaConstant.ACTUAL_START_DATE,t);
                    item.forUdaLessThan(UdaConstant.ACTUAL_START_DATE,toTimePlus1ms(iso));
                    markDirty();
                }
            }
        });

        processOrderFilterBuilder.bind("actualFinish", EnumSet.of(GE, GT, LE, LT, EQ), (filter, op, iso) -> {
            Time t = toTime(iso);
            ProcessOrderItemFilter item = getOrCreateItemFilter(functionsEx);

            switch (op) {
                case GE -> {
                    item.forUdaGreaterThanOrEqualTo(UdaConstant.ACTUAL_FINISH_DATE,t);
                    markDirty();
                }
                case GT -> {
                    item.forUdaGreaterThanOrEqualTo(UdaConstant.ACTUAL_FINISH_DATE,toTimePlus1ms(iso));
                    markDirty();
                }
                case LT -> {
                    item.forUdaLessThan(UdaConstant.ACTUAL_FINISH_DATE,t);
                    markDirty();
                }
                case LE -> {
                    item.forUdaLessThan(UdaConstant.ACTUAL_FINISH_DATE,toTimePlus1ms(iso));
                    markDirty();
                }
                case EQ -> {
                    item.forUdaGreaterThanOrEqualTo(UdaConstant.ACTUAL_FINISH_DATE,t);
                    item.forUdaLessThan(UdaConstant.ACTUAL_FINISH_DATE,toTimePlus1ms(iso));
                    markDirty();
                }
            }
        });


        processOrderFilterBuilder.bind("status", EnumSet.of(EQ, CONTAINS, STARTSWITH), (filter, op, value) -> {
            ProcessOrderItemFilter item = getOrCreateItemFilter(functionsEx);
            switch (op) {
                case EQ -> {
                    item.forCurrentStateEqualTo(value);
                    markDirty();
                }
                case CONTAINS -> {
                    item.forCurrentStateContaining(value);
                    markDirty();
                }
                case STARTSWITH -> {
                    item.forCurrentStateStartingWith(value);
                    markDirty();
                }
            }
        });


        processOrderFilterBuilder.bind("accessPrivilege", EnumSet.of(EQ, CONTAINS), (filter, operation, value) -> {
            ProcessOrderItemFilter item = getOrCreateItemFilter(functionsEx);

            // Si el filtro es nulo o vacío, no aplicamos ningún filtro (mostramos todos)
            if (value == null || value.isEmpty()) {
                return;
            }

            // Construimos el filtro de AccessPrivilege
            AccessPrivilegeFilter privilegeFilter = functionsEx.createAccessPrivilegeFilter();
            switch (operation) {
                case EQ -> privilegeFilter.forNameEqualTo(value);
                case CONTAINS -> privilegeFilter.forNameContaining(value);
            }

            @SuppressWarnings("rawtypes")
            Vector vector = functionsEx.getFilteredAccessPrivileges(privilegeFilter);

            if (vector != null && !vector.isEmpty()) {
                for (Object obj : vector) {
                    if (obj instanceof com.datasweep.compatibility.client.AccessPrivilege accessPrivilege) {
                        item.forUdaEqualTo(UdaConstant.ACCESS_PRIVILEGE, accessPrivilege);
                    }
                }
                markDirty();
            } else {
                // Si el privilegio no existe, lanzamos una excepción descriptiva
                throw new IllegalArgumentException("No data found with accessPrivilege '" + value + "'");
            }
        });

        processOrderFilterBuilder.bind("masterWorkflowName", EnumSet.of(EQ, CONTAINS, STARTSWITH), (filter, op, value) -> {
            MasterRecipeFilter masterRecipeFilter = getOrCreateMasterRecipeFilter(functionsEx);

            switch (op) {
                case EQ -> {
                    masterRecipeFilter.forNameEqualTo(value);
                    markDirty();
                }
                case CONTAINS -> {
                    masterRecipeFilter.forNameContaining(value);
                    markDirty();
                }
                case STARTSWITH -> {
                    masterRecipeFilter.forNameStartingWith(value);
                    markDirty();
                }
            }

        });


        processOrderFilterBuilder.bind("masterWorkflowDescription", EnumSet.of(EQ, CONTAINS, STARTSWITH), (filter, op, value) -> {
            MasterRecipeFilter masterRecipeFilter = getOrCreateMasterRecipeFilter(functionsEx);

            switch (op) {
                case EQ -> {
                    masterRecipeFilter.forDescriptionEqualTo(value);
                    markDirty();
                }
                case CONTAINS -> {
                    masterRecipeFilter.forDescriptionContaining(value);
                    markDirty();
                }
                case STARTSWITH -> {
                    masterRecipeFilter.forDescriptionStartingWith(value);
                    markDirty();
                }
            }
        });

        // ---------- Quantity (value numérico) ----------
        processOrderFilterBuilder.bind("quantity/value", EnumSet.of(GE, GT, LE, LT, EQ), (filter, operation, raw) -> {
            ProcessOrderItemFilter item = getOrCreateItemFilter(functionsEx);
            MeasuredValue mv = toMeasured(filter.getServer(), raw, ApiConstants.DEFAULT_QUANTITY_UOM);
            switch (operation) {
                case GE -> {
                    item.forQuantityGreaterThanOrEqualTo(mv);
                    markDirty();
                }
                case GT -> {
                    item.forQuantityGreaterThan(mv);
                    markDirty();
                }
                case LE -> {
                    item.forQuantityLessThanOrEqualTo(mv);
                    markDirty();
                }
                case LT -> {
                    item.forQuantityLessThan(mv);
                    markDirty();
                }
                case EQ -> {
                    item.forQuantityGreaterThanOrEqualTo(mv);
                    item.forQuantityLessThanOrEqualTo(mv);
                    markDirty();
                }
            }
        });

        return processOrderFilterBuilder.build();
    }

    private static MeasuredValue toMeasured(ServerImpl server, String raw, String defaultUom) throws Exception {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("MeasuredValue vacío");
        }
        String valueRaw = raw.trim();
        String uom = defaultUom;

        valueRaw = valueRaw.replace('|', ' ');

        int splitAt = -1;
        for (int i = 0; i < valueRaw.length(); i++) {
            char c = valueRaw.charAt(i);
            if (!(Character.isDigit(c) || c == '.' || c == ',' || c == '-')) {
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
            String[] toks = valueRaw.split("\\s+", 2);
            numberPart = toks[0].trim();
            if (toks.length > 1 && !toks[1].isBlank()) uom = toks[1].trim();
        }

        numberPart = numberPart.replace(',', '.');
        BigDecimal value = new BigDecimal(numberPart);

        return MeasuredValues.of(server, value, uom);
    }

}
