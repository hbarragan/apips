package com.adasoft.pharmasuite.apips.api.order.domain;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
@Schema(description = "Filter parameters to obtain details of a specific order.")
public class OrderDetailFilter{
    @Parameter(
            name = "key",
            in = ParameterIn.QUERY,
            description = "Unique identifier (key) of the order.",
            example = "18261430",
            explode = Explode.TRUE
    )
    private Long key;

    @Parameter(
            name = "name",
            in = ParameterIn.QUERY,
            example = "800000200",
            description = "Order name (usually for human-readable identification).",
            explode = Explode.TRUE
    )
    private String name;

}

