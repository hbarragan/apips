package com.adasoft.pharmasuite.apips.api.recipe.util;

import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.datasweep.compatibility.client.MasterRecipeFilter;
import com.datasweep.plantops.common.constants.filtering.IMasterRecipeFilterAttributes;
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
public class RecipeOrderByApplier {
    private static final short ASC  = 1;
    private static final short DESC = 2;

    /** Tiempo de creación en MasterRecipeFilter usa atributo 7 (según libs). */
    private static final short ATTR_CREATION_TIME = 7;

    public record OrderClause(String path, boolean asc) {}

    /**
     * @return true si todas las cláusulas se aplicaron en Rockwell; false si alguna no es soportada.
     */
    public boolean applyToRockwell(MasterRecipeFilter filter, OrderByOption option) {
        List<OrderClause> clauses = parse(option);
        if (clauses.isEmpty()) return true;

        boolean allSupported = clauses.stream().allMatch(RecipeOrderByApplier::isSupportedByRockwell);
        if (!allSupported) return false; // que el caller ordene en memoria

        for (OrderClause clause : clauses) {
            String path = clause.path();
            boolean asc = clause.asc();
            path = path.trim().toLowerCase(Locale.ROOT);

            switch (path) {
                case "name" -> filter.orderByName(asc);
                case "description" -> filter.orderByDescription(asc);
                case "revision" -> filter.addOrderBy(IMasterRecipeFilterAttributes.REVISION, asc ? ASC : DESC);
                case "creationdate" -> filter.addOrderBy(ATTR_CREATION_TIME, asc ? ASC : DESC);
                case "effectivitystarttime" ->
                        filter.addOrderBy(IMasterRecipeFilterAttributes.EFFECTIVITY_START_TIME, asc ? ASC : DESC);
                case "effectivityendtime" ->
                        filter.addOrderBy(IMasterRecipeFilterAttributes.EFFECTIVITY_END_TIME, asc ? ASC : DESC);
                case "accessprivilege" ->
                        filter.orderByUda(UdaConstant.ACCESS_PRIVILEGE, asc);

                // NOTA: cualquier ruta compleja o campos de hijos (material/name, quantity.value, procedureName, etc.)
                // NO se soportan server-side en MasterRecipeFilter → no deberían llegar aquí porque
                // isSupportedByRockwell() devolvería false.
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

    private static boolean isSupportedByRockwell(OrderClause orderClause) {
        String path = orderClause.path();

        // Soportados directamente por MasterRecipeFilter:
        // - name (orderByName)
        // - description (orderByDescription)
        // - revision (addOrderBy(IMasterRecipeFilterAttributes.REVISION, ...))
        // - creationDate (addOrderBy((short)7, ...))
        // - effectivityStartTime / effectivityEndTime (addOrderBy con 10/11)
        // - accessPrivilege (UDA en MR: orderByUda)
        switch (path) {
            case "name":
            case "description":
            case "revision":
            case "creationDate":
            case "effectivityStartTime":
            case "effectivityEndTime":
            case "accessPrivilege":
                return true;

            default:
                // NO soportados server-side:
                // - status (estado actual) → no hay orderBy en MR
                // - materialName / materialDescription → pertenecen a Part (subfiltro)
                // - quantity.* → UDA en Part
                // - procedureName → viene de IMESProcedure (subfiltro)
                // - rutas complejas tipo "material/name", "quantity/value", etc.
                return false;
        }
    }
}
