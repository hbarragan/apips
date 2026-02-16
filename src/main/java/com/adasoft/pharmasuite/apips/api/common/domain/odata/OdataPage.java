package com.adasoft.pharmasuite.apips.api.common.domain.odata;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
@Data
@Schema(description = "Parameters for OData v4 pagination and filtering.")
public class OdataPage {

    @Parameter(
            name = "$filter",
            description = "OData filter expression. To return all, leave empty. Use ISO 8601-UTC for dates.",
            example = "CreationTime ge 2025-01-01T00:00:00Z and CreationTime le 2025-01-31T23:59:59Z"
    )
    String odataFilter;

    @Parameter(
            name = "$orderby",
            description = "OData order by expression. To return all, leave empty.",
            example = "CreationTime desc"
    )
    String orderBy;

    @Parameter(
            name = "$top",
            description = "Maximum number of records to return. To return all, leave empty.",
            example = "10"
    )
    Integer top;

    @Parameter(
            name = "$skip",
            description = "Number of records to skip. To return all, leave empty.",
            example = "0"
    )
    Integer skip;

    @Parameter(
            name = "$count",
            description = "Whether to include the total count of matching records in the response",
            example = "true"
    )
    Boolean count;

    transient HttpServletRequest request;
}
