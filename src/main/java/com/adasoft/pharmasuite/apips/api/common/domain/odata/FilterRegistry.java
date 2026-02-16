package com.adasoft.pharmasuite.apips.api.common.domain.odata;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class FilterRegistry<F> {
    private final Map<String, Binder<F>> handlers;
    private final Function<String, String> keyNormalizer;

    public enum Op { EQ, NE, GT, GE, LT, LE, CONTAINS, STARTSWITH, ENDSWITH }

    @FunctionalInterface
    public interface Binder<T> {
        void apply(T filter, Op op, String value) throws Exception;
    }

    private FilterRegistry(Map<String, Binder<F>> handlers,
                           Function<String, String> keyNormalizer) {
        this.handlers = handlers;
        this.keyNormalizer = keyNormalizer;
    }

    public Optional<Binder<F>> find(String rawKey) {
        String k = keyNormalizer.apply(rawKey);
        return Optional.ofNullable(handlers.get(k));
    }

    public static final class Builder<T> {
        private final Map<String, Binder<T>> handlers = new HashMap<>();
        private Function<String, String> keyNormalizer = Function.identity();

        /** Activa modo case-insensitive (normaliza a lower-case). */
        public Builder<T> caseInsensitiveKeys() {
            this.keyNormalizer = s -> s == null ? null : s.toLowerCase(Locale.ROOT);
            return this;
        }

        public Builder<T> bind(String key, EnumSet<Op> ops, Binder<T> binder) {
            // Registramos con la clave normalizada
            String nk = keyNormalizer.apply(key);
            handlers.put(nk, (f, op, v) -> {
                if (!ops.contains(op)) {
                    throw new IllegalArgumentException("Operador no soportado: " + op + " para " + key);
                }
                binder.apply(f, op, v);
            });
            return this;
        }

        public FilterRegistry<T> build() {
            return new FilterRegistry<>(new HashMap<>(handlers), keyNormalizer);
        }
    }

}
