package com.adasoft.pharmasuite.apips.api.order.util;

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

public class ProcessOrderOrderByApplier {
    private static final short ASC  = 1;
    private static final short DESC = 2;

    public record OrderClause(String path, boolean asc) {}

    public boolean applyToRockwell(ProcessOrderFilter processOrderFilter, OrderByOption option) {
        List<OrderClause> clauses = parse(option);
        if (clauses.isEmpty()) return true;

        boolean allSupported = clauses.stream().allMatch(ProcessOrderOrderByApplier::isSupportedByRockwell);

        if (!allSupported) return false;

        for (OrderClause c : clauses) {
            String p = c.path().trim().toLowerCase(Locale.ROOT);
            boolean asc = c.asc();

            switch (p) {
                case "name" -> processOrderFilter.addOrderBy((short)3, asc ? ASC : DESC);
                case "creationdate" -> processOrderFilter.addOrderBy((short)7, asc ? ASC : DESC);
                default -> throw new IllegalStateException("Ruta soportada no contemplada: " + p);
            }
        }
        return true;
    }

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
        String p = c.path().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "name", "creationdate" -> true;
            default -> false;
        };
    }

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
