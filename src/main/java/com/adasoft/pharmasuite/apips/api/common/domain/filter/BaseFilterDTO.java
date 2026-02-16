package com.adasoft.pharmasuite.apips.api.common.domain.filter;


import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.tool.annotation.ToolParam;

@Getter
@Setter
public class BaseFilterDTO {
    @ToolParam(
            description = "Number of months backwards from the current date used to filter results. " +
                    "If provided, results will include records created within the last N months. " +
                    "If not specified, the default interval defined in the application configuration will be used.",
            required = false
    )
    private Integer lastMonthsInterval;

    @ToolParam(
            description = "Initial creation date (inclusive) used as the lower bound for filtering results. " +
                    "The date must be provided in ISO 8601 UTC format (e.g. '2025-01-01T00:00:00Z'). " +
                    "If not specified, the filter will not apply a start date constraint.",
            required = false
    )
    private String initCreationDate;

    @ToolParam(
            description = "Final creation date (inclusive) used as the upper bound for filtering results. " +
                    "The date must be provided in ISO 8601 UTC format (e.g. '2025-01-31T23:59:59Z'). " +
                    "If not specified, the filter will not apply an end date constraint.",
            required = false
    )
    private String finishCreationDate;
}