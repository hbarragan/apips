package com.adasoft.pharmasuite.apips.api.common.domain.odata;

import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.Server;

import java.math.BigDecimal;

public final class MeasuredValues extends MeasuredValue {

    // Constructor público que encadena al protected del SDK
    public MeasuredValues(BigDecimal value, String uomSymbol, int scale, Server server) throws Exception {
        super(value, uomSymbol, scale, server);
    }

    /** Factory conveniente con escala derivada del valor */
    public static MeasuredValue of(Server server, BigDecimal value, String uomSymbol) throws Exception {
        int scale = value.scale() < 0 ? 0 : value.scale();
        return new MeasuredValues(value, uomSymbol, scale, server);
    }
}
