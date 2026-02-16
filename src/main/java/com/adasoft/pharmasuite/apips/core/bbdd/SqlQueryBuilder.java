package com.adasoft.pharmasuite.apips.core.bbdd;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SqlQueryBuilder {
    private final String template;
    private final Map<String,Object> params = new LinkedHashMap<>();

    private static final Pattern IN_CLAUSE_WITH_CONJ_PATTERN = Pattern.compile(
            "(?i)\\s*(AND|OR)\\s+[^\\r\\n]*?:%s\\b[^\\r\\n]*"
    );
    private static final Pattern PARAM_PLACEHOLDER = Pattern.compile(":(\\w+)\\b");

    public SqlQueryBuilder(String template) {
        this.template = template;
    }

    public SqlQueryBuilder with(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public String build() {
        String sql = template;

        // 1) Primero manejar colecciones: listas → IN (v1,v2,…), o eliminar AND…IN si vacío/null
        for (String name : extractParamNames(template)) {
            Object val = params.get(name);
            // patrón específico para este parámetro
            Pattern conjPattern = Pattern.compile(
                    String.format(IN_CLAUSE_WITH_CONJ_PATTERN.pattern(), name)
            );
            if (val instanceof Collection<?>) {
                Collection<?> col = (Collection<?>) val;
                if (col.isEmpty()) {
                    sql = conjPattern.matcher(sql).replaceAll("");
                } else {
                    String literalList = col.stream()
                            .map(SqlQueryBuilder::formatValue)
                            .collect(Collectors.joining(", "));
                    // reemplazo simple de placeholder
                    sql = sql.replaceAll("(?i):" + name + "\\b", "(" + literalList + ")");
                }
            }
        }

        // 2) Después formatear escalares y eliminar sus cláusulas vacías si vienen null
        for (String name : extractParamNames(template)) {
            if (!params.containsKey(name) || params.get(name) == null) {
                Pattern conjPattern = Pattern.compile("(?i)\\s*(AND|OR)\\s+[^\\r\\n]*?:"+ name + "\\b[^\\r\\n]*");
                sql = conjPattern.matcher(sql).replaceAll("");
            } else {
                Object val = params.get(name);
                if (!(val instanceof Collection<?>)) {
                    String literal = formatValue(val);
                    sql = sql.replaceAll("(?i):" + name + "\\b", literal);
                }
            }
        }

        // 3) Limpieza básica de espacios y saltos
        sql = sql.replaceAll("[ \\t]{2,}", " ").replaceAll("\\r?\\n", " ").trim();
        return sql;
    }

    /** Extrae nombres :param de la plantilla */
    private Set<String> extractParamNames(String tpl) {
        Matcher m = PARAM_PLACEHOLDER.matcher(tpl);
        Set<String> names = new LinkedHashSet<>();
        while (m.find()) names.add(m.group(1));
        return names;
    }

    private static String formatValue(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number) return v.toString();
        String s = v.toString().replace("'", "''");
        return "'" + s + "'";
    }
}
