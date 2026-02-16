package com.adasoft.pharmasuite.apips.api.common.domain.filter;

import com.adasoft.pharmasuite.apips.api.common.domain.HelperSchema;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseFilter{


    @Schema(description = "Range in months to filter. The default value in application.yml is 5. Its highly recommended to use this filter for improve querys.",
            example = "3")
    @Parameter()
    private Integer lastMonthsInterval;

    @Schema(
            description = "Filter by creation date in ISO 8601-UTC. Example: '2025-01-01T23:59:59Z'. Format: 'yyyy-MM-ddTHH:mm:ssX\n'",
            example = "2025-01-01T23:59:59Z"
    )
    @Parameter()
    private String initCreationDate;

    @Schema(
            description = "Filter by finish creation date in ISO 8601-UTC. '2025-01-31T23:59:59Z' Format: 'yyyy-MM-ddTHH:mm:ssX\n'",
            example = "2025-01-31T23:59:59Z"
    )
    @Parameter()
    private String finishCreationDate;

    protected static Map<String, Object> buildBaseMapSchema(Map<String, Object> map) {
        map.put( "lastMonthsInterval", 5);
        map.put( "initCreationDate","2025-01-01T23:59:59Z");
        map.put( "finishCreationDate","2025-01-31T23:59:59Z");
        return map;
    }


}
