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
@Schema(description = "Filter parameters for orderProcess queries paged.")
public class OrderFilterPaged extends BaseFilterCache {

    @Parameter(
            name = "status",
            in = ParameterIn.QUERY,
            description = "Filter by order status. Multiple values can be provided.",
            explode = Explode.TRUE,
            schema = @Schema(
                    type = "array",
                    allowableValues = {"defined", "exploded", "released", "inprocess", "finished", "annulled", "cancelled", "reactivated", "productionreviewed", "reviewed"},
                    description = "Order status list"
            )
    )
    private List<String> status;

    @Schema(
            description = "Page number to retrieve (1-based).",
            example = "1"
    )
    @Parameter(description = "Page number")
    private Integer numPage;

    @Schema(
            description = "Number of records per page.",
            example = "20"
    )
    @Parameter(description = "Page size")
    private Integer numRows;


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
            description = "If true, it will take longer because for each record it will make a query to obtain more details",
            example = "false"
    )
    @Parameter()
    private boolean all = false;

}
