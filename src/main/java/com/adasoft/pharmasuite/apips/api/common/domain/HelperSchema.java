package com.adasoft.pharmasuite.apips.api.common.domain;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.*;

import java.util.*;

public class HelperSchema {

    public static Map<String, Object> minimalInputSchema(
            Class<?> clazz,
            Map<String, Object> knownDefaults,
            Collection<String> ignoreFields
    ) {
        var resolved = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(clazz));
        Schema<?> root = resolved.schema;

        Map<String, Schema<?>> props = collectAllProperties(root);

        Set<String> ignore = (ignoreFields == null)
                ? Set.of()
                : new HashSet<>(ignoreFields);

        Map<String, Object> properties = new LinkedHashMap<>();
        for (var e : props.entrySet()) {
            String name = e.getKey();

            // ✅ aquí se ignora
            if (ignore.contains(name)) continue;

            Schema<?> s = e.getValue();

            Map<String, Object> field = new LinkedHashMap<>();
            field.put("type", detectType(s));

            if (s.getDescription() != null && !s.getDescription().isBlank())
                field.put("description", s.getDescription());
            if (s.getExample() != null)
                field.put("example", s.getExample());
            if (s.getFormat() != null && !s.getFormat().isBlank())
                field.put("format", s.getFormat());
            if (s.getEnum() != null && !s.getEnum().isEmpty())
                field.put("enum", new ArrayList<>(s.getEnum()));

            if (s instanceof ArraySchema as) {
                Schema<?> items = as.getItems();
                Map<String, Object> itemsMap = new LinkedHashMap<>();
                itemsMap.put("type", detectType(items));
                if (items != null && items.getEnum() != null && !items.getEnum().isEmpty()) {
                    itemsMap.put("enum", new ArrayList<>(items.getEnum()));
                }
                if (items != null && items.getFormat() != null)
                    itemsMap.put("format", items.getFormat());
                field.put("items", itemsMap);
            }

            if (knownDefaults != null && knownDefaults.containsKey(name)) {
                field.put("default", knownDefaults.get(name));
            } else if (s.getDefault() != null) {
                field.put("default", s.getDefault());
            }

            if ("string".equals(field.get("type")) &&
                    !field.containsKey("format") &&
                    looksLikeDateField(name)) {
                field.put("format", "date-time");
            }

            properties.put(name, prune(field));
        }

        Map<String, Object> rootMap = new LinkedHashMap<>();
        rootMap.put("type", "object");
        rootMap.put("properties", properties);
        rootMap.put("additionalProperties", false);
        return rootMap;
    }


    /* ---------- helpers ---------- */

    @SuppressWarnings("unchecked")
    private static Map<String, io.swagger.v3.oas.models.media.Schema<?>> collectAllProperties(io.swagger.v3.oas.models.media.Schema<?> schema) {
        Map<String, io.swagger.v3.oas.models.media.Schema<?>> result = new LinkedHashMap<>();
        if (schema instanceof ComposedSchema cs && cs.getAllOf() != null) {
            for (io.swagger.v3.oas.models.media.Schema<?> part : cs.getAllOf()) {
                result.putAll(collectAllProperties(part));
            }
        }
        if (schema.getProperties() != null) {
            result.putAll((Map<String, io.swagger.v3.oas.models.media.Schema<?>>) (Map<?, ?>) schema.getProperties());
        }
        return result;
    }

    private static String detectType(io.swagger.v3.oas.models.media.Schema<?> s) {
        if (s instanceof StringSchema) return "string";
        if (s instanceof IntegerSchema) return "integer";
        if (s instanceof NumberSchema) return "number";
        if (s instanceof BooleanSchema) return "boolean";
        if (s instanceof ArraySchema) return "array";
        return "object";
    }

    private static boolean looksLikeDateField(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("date") || n.contains("creation");
    }

    private static Map<String, Object> prune(Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (var e : m.entrySet()) {
            Object v = e.getValue();
            if (v == null) continue;
            if (v instanceof String s && s.isBlank()) continue;
            if (v instanceof Map<?, ?> mm) {
                Map<String, Object> pruned = prune((Map<String, Object>) mm);
                if (!pruned.isEmpty()) out.put(e.getKey(), pruned);
            } else if (v instanceof Collection<?> c) {
                if (!c.isEmpty()) out.put(e.getKey(), v);
            } else {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }
}