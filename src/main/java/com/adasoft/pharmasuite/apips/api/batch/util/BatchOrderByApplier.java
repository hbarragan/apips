package com.adasoft.pharmasuite.apips.api.batch.util;

import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.datasweep.compatibility.client.BatchFilter;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BatchOrderByApplier {
    private static final short ASC  = 1;
    private static final short DESC = 2;

    public record OrderClause(String path, boolean asc) {}

    /** Devuelve true si TODAS las cláusulas fueron soportadas y aplicadas en origen. */
    public boolean applyToRockwell(BatchFilter batchFilter, OrderByOption option) {
        List<OrderClause> clauses = parse(option);
        if (clauses.isEmpty()) return true;

        // Ver qué cláusulas podemos empujar a Rockwell
        boolean allSupported = clauses.stream().allMatch(BatchOrderByApplier::isSupportedByRockwell);

        if (!allSupported) return false; // que el caller haga ordenación en memoria

        // Todas soportadas → aplicar en el mismo orden
        for (OrderClause c : clauses) {
            String p = c.path();
            boolean asc = c.asc();
            p = p.trim().toLowerCase(Locale.ROOT);

            switch (p) {
                case "name"                         -> batchFilter.orderByBatchName(asc);
                case "description"                  -> batchFilter.orderByDescription(asc);
                case "category"                     -> batchFilter.orderByCategory(asc);
                case "quantity", "quantity/value"   -> batchFilter.orderByQuantity(asc);
                case "potency",  "potency/value"    -> batchFilter.orderByPotency(asc);

                // Atributos temporales (IDs deducidos por los métodos de filtro):
                // creationTime usa atributo 7 en los métodos forCreationTime...
                case "creationtime"                 -> batchFilter.addOrderBy((short)7,  asc ? ASC : DESC);
                // expiryDate/expirationTime usa atributo 10 en los métodos forExpirationTime...
                case "expirydate", "expirationtime" -> batchFilter.addOrderBy((short)10, asc ? ASC : DESC);
                case "accessprivilege"              -> batchFilter.orderByUda(UdaConstant.ACCESS_PRIVILEGE,asc);

                default -> throw new IllegalStateException("Ruta soportada no contemplada: " + p);
            }
        }
        return true;
    }

    /** Extrae rutas como "name", "quantity/value", "material/name"... y dirección asc/desc. */
    public static List<OrderClause> parse(OrderByOption opt) {
        List<OrderClause> out = new ArrayList<>();
        if (opt == null || opt.getOrders() == null) return out;

        for (OrderByItem item : opt.getOrders()) {
            Expression exp = item.getExpression();
            if (!(exp instanceof Member m)) {
                // En OData V4 lo normal para orderby es Member → property path.
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
            case "name", "description", "category",
                 "quantity", "quantity/value",
                 "potency",  "potency/value",
                 "creationTime",
                 "expiryDate", "expirationTime",
                    "accessPrivilege"-> true;
            default -> false; // p.ej. "material/name", "status", "productionDate", etc.
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
}
