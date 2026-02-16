package com.adasoft.pharmasuite.apips.api.workflow.util;

import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.datasweep.compatibility.client.ProcessOrderFilter;
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



/**
 *
 * Devolvemos false cuando hay cláusulas para que el caller ordene en colección OData.
 */
public class WorkflowOrderByApplier {
    private static final short ASC  = 1;
    private static final short DESC = 2;


    public record OrderClause(String path, boolean asc) {}


    public boolean applyToRockwell(ProcessOrderFilter filter, OrderByOption option) {
        List<OrderClause> clauses = parse(option);
        if (clauses.isEmpty()) return true; // nada que ordenar


        boolean allSupported = clauses.stream().allMatch(WorkflowOrderByApplier::isSupportedByRockwell);
        if (!allSupported) return false; // que el caller ordene en memoria


        for (OrderClause clause : clauses) {
            String path = clause.path();
            boolean asc = clause.asc();
            path = path.trim().toLowerCase(Locale.ROOT);
            switch (path) {
                case "name" -> filter.addOrderBy((short)3, asc ? ASC : DESC);
                case "description" -> filter.orderByDescription(asc);
                case "creationdate" -> filter.orderByCreationTime(asc);
                case "accessprivilege" -> filter.orderByUda(UdaConstant.ACCESS_PRIVILEGE, asc);
                default -> throw new IllegalStateException("Ruta soportada no contemplada: " + path);
            }
        }
        return true;
    }


    public static List<OrderClause> parse(OrderByOption option) {
        List<OrderClause> out = new ArrayList<>();
        if (option == null || option.getOrders() == null) return out;
        for (OrderByItem item : option.getOrders()) {
            Expression exp = item.getExpression();
            if (!(exp instanceof Member m)) {
                throw new IllegalArgumentException("Expresión $orderby no soportada: " + exp.getClass());
            }
            String path = resolvePath(m.getResourcePath());
            out.add(new OrderClause(path, !item.isDescending()));
        }
        return out;
    }


    private static String resolvePath(UriInfoResource resources) {
        List<UriResource> parts = resources.getUriResourceParts();
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

    private static boolean isSupportedByRockwell(WorkflowOrderByApplier.OrderClause orderClause) {
        String path = orderClause.path();
        return switch (path) {
            case "name",
                 "description",
                 "creationDate",
                 "accessPrivilege" -> true;
            default -> false;
        };
    }
}
