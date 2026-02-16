package com.adasoft.pharmasuite.apips.api.common.domain.odata;

import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.Server;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MeasuredValues extends MeasuredValue {

        private static final int MAX_SCALE = 6;

        // Constructor público que encadena al protected del SDK
        public MeasuredValues(BigDecimal value, String uomSymbol, int scale, Server server) throws Exception {
            super(value, uomSymbol, scale, server);
        }

        /** Factory conveniente con escala derivada del valor */
        public static MeasuredValue of(Server server, BigDecimal value, String uomSymbol) throws Exception {

            if (value == null) {
                throw new IllegalArgumentException("MeasuredValue value no puede ser null");
            }

            BigDecimal normalized = value.stripTrailingZeros();
            if (normalized.scale() < 0) {
                normalized = normalized.setScale(0);
            }

            if (normalized.scale() > MAX_SCALE) {
                normalized = normalized.setScale(MAX_SCALE, RoundingMode.HALF_UP);
            }

            return new MeasuredValues(normalized, uomSymbol, normalized.scale(), server);
        }
    }
