package com.adasoft.pharmasuite.apips.api.order.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.HelperSchema;
import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterCache;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = false)
@Data
@Schema(description = "Filter parameters for orderProcess queries.")
public class OrderFilter extends BaseFilterCache {
    @Schema(
            description = "Filter by order name (exact or partial depending on implementation)",
            example = "M300002"
    )
    @Parameter(description = "Order name filter")
    private String name;

    @Parameter(
            name = "status",
            in = ParameterIn.QUERY,
            description = "Filter by order status. Multiple values can be provided.",
            explode = Explode.TRUE,
            schema = @Schema(
                    type = "array",
                    allowableValues = {"Defined", "Exploded", "Released", "In process", "Finished", "Annulled", "Cancelled", "Reactivated", "ProductionReviewed", "Reviewed"},
                    description = "Order status list"
            )
    )
    private List<String> status;

    @Parameter(
            name = "erpStartDate",
            in = ParameterIn.QUERY,
            example = "2024-06-20T23:59:59Z",
            description = "Filter by ERP start date in ISO 8601-UTC format (yyyy-MM-ddTHH:mm:ssX)",
            explode = Explode.TRUE
    )
    private String erpStartDate;

    @Parameter(
            name = "erpFinishDate",
            in = ParameterIn.QUERY,
            example = "2025-06-20T23:59:59Z",
            description = "Filter by ERP finish date in ISO 8601-UTC format (yyyy-MM-ddTHH:mm:ssX)",
            explode = Explode.TRUE
    )
    private String erpFinishDate;

    @Parameter(
            name = "accessPrivilege",
            in = ParameterIn.QUERY,
            example = "JC_Visualizacion",
            description = "Filter by access privilege",
            explode = Explode.TRUE
    )
    private String accessPrivilege;

    @Schema(
            description = "Indicates whether to retrieve extended order details. " +
                    "If set to true, additional data such as consumedQuantity, workCenter, " +
                    "location and consumed quantities will be included. " +
                    "Enabling this option may increase response time, as additional queries " +
                    "are executed per record. It is recommended to use true only when detailed " +
                    "information is strictly required.",
            example = "false"
    )
    @Parameter(
            name = "all",
            example = "false"
    )
    private boolean all = false;


    @Override
    public Map<String, Object> getSchemaSwagger(Collection<String> ignoreFields) {
        return HelperSchema.minimalInputSchema(
                this.getClass(),
                buildBaseMapSchema(
                        new HashMap<>(Map.of(
                                "status", "defined,exploded,released, inprocess,finished,annulled,cancelled,reactivate,productionreviewed,reviewed",
                                "name", "M300002",
                                "erpStartDate", "2025-01-01T00:00:00Z",
                                "erpFinishDate", "2025-01-31T23:59:59Z",
                                "accessPrivilege", "JC_Visualizacion"
                        ))
                ),
                ignoreFields
        );
    }

}

