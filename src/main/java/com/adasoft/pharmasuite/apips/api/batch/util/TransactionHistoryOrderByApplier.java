package com.adasoft.pharmasuite.apips.api.batch.util;

import com.rockwell.mes.services.inventory.ifc.TransactionHistoryObject;
import com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TransactionHistoryOrderByApplier {

    private static final short ASC  = 1;
    private static final short DESC = 2;

    /** En filtros Rockwell/Datasweep: 2 = KEY, 7 = ATCOLUMN */
    private static final short ATTR_KEY     = 2;
    private static final short ATTR_ATCOL   = 7;


    private static final String COL_SUBLOT_OLD =
            getStaticStringField(TransactionHistoryObject.class, "COL_NAME_SUBLOT_OLD", "X_sublotIdentifierOld");

    public record OrderClause(String path, boolean asc) {}

    /**
     * @return true si TODAS las cláusulas se aplicaron en Rockwell; false si alguna no es soportada.
     */
    public boolean applyToRockwell(TransactionHistoryFilter filter, OrderByOption option) {
        List<OrderClause> clauses = parse(option);
        if (clauses.isEmpty()) return true;

        boolean allSupported = clauses.stream().allMatch(TransactionHistoryOrderByApplier::isSupportedByRockwell);
        if (!allSupported) return false; // que el caller ordene en memoria

        for (OrderClause c : clauses) {
            String p = c.path().trim().toLowerCase(Locale.ROOT);
            short order = c.asc() ? ASC : DESC;

            switch (p) {
                case "time" -> {
                    // Orden estable para paginación: timestamp + creationSeqIndex
                    addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_TIMESTAMP, order);
                    addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_CREATIONSEQINDEX, order);
                }
                case "type" -> addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_TRANSACTION_TYPE, order);
                case "subtype" -> addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_TRANSACTION_SUBTYPE, order);

                case "orderid" -> addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_ORDER, order);
                case "orderstep" -> addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_ORDER_STEP, order);

                case "batchidnew" -> addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_BATCH_NEW, order);
                case "batchidold" -> addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_BATCH_OLD, order);

                case "sublotidnew" -> addOrderAtColumn(filter, TransactionHistoryObject.COL_NAME_SUBLOT_NEW, order);
                case "sublotidold" -> addOrderAtColumn(filter, COL_SUBLOT_OLD, order);

                default -> throw new IllegalStateException("Ruta soportada no contemplada: " + p);
            }
        }
        return true;
    }

    /** Extrae rutas como "time", "orderId"... y dirección asc/desc. */
    public static List<OrderClause> parse(OrderByOption opt) {
        List<OrderClause> out = new ArrayList<>();
        if (opt == null || opt.getOrders() == null) return out;

        for (OrderByItem item : opt.getOrders()) {
            Expression exp = item.getExpression();
            if (!(exp instanceof Member m)) {
                throw new IllegalArgumentException("Expresión $orderby no soportada: " + exp.getClass());
            }
            String path = resolvePath(m.getResourcePath());
            out.add(new OrderClause(path, !item.isDescending()));
        }
        return out;
    }

    private static boolean isSupportedByRockwell(OrderClause c) {
        String p = c.path();
        return switch (p) {
            case "time", "type", "subtype",
                 "orderId", "orderStep",
                 "batchIdNew", "batchIdOld",
                 "sublotIdNew", "sublotIdOld" -> true;
            default -> false;
        };
    }

    /** Convierte UriResourceParts → "prop/subprop" (solo complejas+primitivas). */
    private static String resolvePath(UriInfoResource res) {
        List<UriResource> parts = res.getUriResourceParts();
        List<String> names = new ArrayList<>(parts.size());
        for (UriResource r : parts) {
            if (r instanceof UriResourcePrimitiveProperty pp) {
                names.add(pp.getProperty().getName());
            } else if (r instanceof UriResourceComplexProperty cp) {
                names.add(cp.getProperty().getName());
            }
        }
        String path = String.join("/", names);
        return Objects.requireNonNullElse(path, "");
    }

    /**
     * Añade un order-by por AT column.
     * Probamos métodos típicos en filtros Rockwell:
     * - addOrderATColumnBy(String col, short sortOrder)
     * - addOrderBy(String attributeIdentifier, short attributeType, short sortOrder) con attributeType=7
     */
    private static void addOrderAtColumn(TransactionHistoryFilter filter, String col, short sortOrder) {
        if (col == null || col.isBlank()) return;

        // 1) addOrderATColumnBy(String, short)
        if (invokeIfExists(filter, "addOrderATColumnBy",
                new Class<?>[]{String.class, short.class},
                new Object[]{col, sortOrder})) {
            return;
        }

        // 2) addOrderBy(String, short, short)
        if (invokeIfExists(filter, "addOrderBy",
                new Class<?>[]{String.class, short.class, short.class},
                new Object[]{col, ATTR_ATCOL, sortOrder})) {
            return;
        }

        throw new IllegalStateException("TransactionHistoryFilter no soporta order-by AT column (no existe addOrderATColumnBy/addOrderBy(String,short,short)).");
    }

    private static boolean invokeIfExists(Object target, String method, Class<?>[] paramTypes, Object[] args) {
        try {
            Method m = target.getClass().getMethod(method, paramTypes);
            m.invoke(target, args);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("Error invocando " + method + " en " + target.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    private static String getStaticStringField(Class<?> clazz, String fieldName, String fallback) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != String.class) return fallback;
            f.setAccessible(true);
            Object v = f.get(null);
            if (v instanceof String s && !s.isBlank()) return s;
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
