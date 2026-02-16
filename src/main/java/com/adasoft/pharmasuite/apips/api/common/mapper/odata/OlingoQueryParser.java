package com.adasoft.pharmasuite.apips.api.common.mapper.odata;

import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.commons.core.edm.EdmProviderImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.core.uri.parser.Parser;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OlingoQueryParser {
    private final OData odata;
    private final org.apache.olingo.commons.api.edm.Edm edm;
    private final String entitySetPath;
    private static final Pattern LITERAL = Pattern.compile("'([^']|'')*'");

    public OlingoQueryParser(CsdlEdmProvider edmProvider, String entitySetPath) {
        this.odata = OData.newInstance();
        this.edm   = new EdmProviderImpl(edmProvider);
        this.entitySetPath = entitySetPath;
    }

    /** Parsea TODA la query string (sin '?'): $filter&$top&$skip&$orderby&$count... */
    public UriInfo parseQuery(String rawQuery) throws UriValidationException, UriParserException {
        String q = (rawQuery == null) ? "" : rawQuery;
        String normalized = canonicalizeQuery(q);
        return new Parser(edm, odata).parseUri(entitySetPath, normalized, null, null);
    }

    public org.apache.olingo.commons.api.edm.Edm edm() { return edm; }
    public OData odata() { return odata; }


    // =========================
    //   Normalización case-insensitive
    // =========================

    private String canonicalizeQuery(String query) {
        Map<String,String> canon = resolveCanonicalPropertyMap();
        if (canon.isEmpty() || query == null || query.isBlank()) return query;

        // Partimos por '&' para tratar cada opción por separado
        String[] parts = query.split("&");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            int eq = p.indexOf('=');
            if (eq < 0) continue;

            String key = p.substring(0, eq);
            String val = p.substring(eq + 1);

            switch (key) {
                case "$filter"  -> val = normalizeFilter(val, canon);
                case "$orderby" -> val = normalizeOrderBy(val, canon);
                case "$select"  -> val = normalizeSelect(val, canon);
                default -> { /* otras opciones no necesitan normalización */ }
            }
            parts[i] = key + "=" + val;
        }
        return String.join("&", parts);
    }

    private Map<String,String> resolveCanonicalPropertyMap() {
        try {
            EdmEntityContainer container = edm.getEntityContainer();
            if (container == null) return Map.of();
            EdmEntitySet es = container.getEntitySet(entitySetPath);
            if (es == null) return Map.of();
            EdmEntityType et = es.getEntityType();
            if (et == null) return Map.of();

            // Solo propiedades estructurales. Si necesitas nav props, añade et.getNavigationPropertyNames()
            List<String> props = et.getPropertyNames();
            return props.stream().collect(Collectors.toMap(
                    s -> s.toLowerCase(Locale.ROOT),
                    s -> s
            ));
        } catch (Exception ignore) {
            return Map.of();
        }
    }

    /** Reescribe identificadores de propiedades en $filter, evitando tocar literales de texto. */
    private String normalizeFilter(String expr, Map<String,String> canon) {
        if (expr == null || expr.isBlank()) return expr;

        // 1) Extraer literales y sustituir por placeholders
        List<String> literals = new ArrayList<>();
        String skeleton = stripLiterals(expr, literals);

        // 2) Reemplazar fuera de literales: tokens de propiedades con su forma canónica
        String replaced = replaceIdentifiers(skeleton, canon);

        // 3) Restaurar literales
        return restoreLiterals(replaced, literals);
    }

    /** Reescribe la lista de campos en $orderby (campo [asc|desc],...). */
    private String normalizeOrderBy(String val, Map<String,String> canon) {
        if (val == null || val.isBlank()) return val;
        String[] items = val.split(",");
        for (int i = 0; i < items.length; i++) {
            String it = items[i].trim();
            if (it.isEmpty()) continue;

            // campo [asc|desc]
            String[] parts = it.split("\\s+");
            String field = parts[0];
            String canonField = canon.get(field.toLowerCase(Locale.ROOT));
            if (canonField != null) {
                parts[0] = canonField;
                items[i] = String.join(" ", parts);
            }
        }
        return String.join(",", items);
    }

    /** Reescribe la lista de campos en $select (campo1,campo2,...) */
    private String normalizeSelect(String val, Map<String,String> canon) {
        if (val == null || val.isBlank()) return val;
        String[] items = val.split(",");
        for (int i = 0; i < items.length; i++) {
            String field = items[i].trim();
            if (field.isEmpty()) continue;
            String canonField = canon.get(field.toLowerCase(Locale.ROOT));
            if (canonField != null) items[i] = canonField;
        }
        return String.join(",", items);
    }

    // ----- helpers de reescritura fuera de literales -----

    private static String stripLiterals(String s, List<String> outLiterals) {
        Matcher m = LITERAL.matcher(s);
        int last = 0, idx = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(s, last, m.start());
            sb.append("__LIT").append(idx).append("__");
            outLiterals.add(s.substring(m.start(), m.end()));
            last = m.end();
            idx++;
        }
        sb.append(s.substring(last));
        return sb.toString();
    }

    private static String restoreLiterals(String s, List<String> literals) {
        String r = s;
        for (int i = 0; i < literals.size(); i++) {
            r = r.replace("__LIT" + i + "__", literals.get(i));
        }
        return r;
    }

    private static String replaceIdentifiers(String s, Map<String,String> canon) {
        String r = s;
        for (Map.Entry<String,String> e : canon.entrySet()) {
            String lower = e.getKey();
            String canonical = e.getValue();
            // \b para límites de palabra, (?i) case-insensitive; evitamos tocar $keywords (no están en canon)
            String regex = "(?i)\\b" + Pattern.quote(lower) + "\\b";
            r = r.replaceAll(regex, canonical);
        }
        return r;
    }
}
