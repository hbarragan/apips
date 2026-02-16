package com.adasoft.pharmasuite.apips.api.workflow.domain;

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
@Schema(description = "Filter parameters for workflow queries.")
public class WorkflowFilter extends BaseFilterCache {
    @Parameter(
            name = "status",
            in = ParameterIn.QUERY,
            description = "Filter by state. Allowed values: defined, exploded, released, inprocess, finished, annulled, cancelled, reactivated, productionreviewed, reviewed",
            explode = Explode.TRUE,
            schema = @Schema(type = "array", allowableValues = {"defined", "exploded", "released", "inprocess", "finished", "annulled", "cancelled", "reactivated", "productionreviewed", "reviewed"})
    )
    private List<String> status;

    @Parameter(
            name = "accessPrivilege",
            in = ParameterIn.QUERY,
            example = "ESCU_LH_Visualización",
            description = "Filter by access privilege",
            explode = Explode.TRUE
    )
    private String accessPrivilege;

    public Map<String, Object> getSchemaSwagger(Collection<String> ignoreFields) {
        return HelperSchema.minimalInputSchema(
                this.getClass(),
                buildBaseMapSchema(
                        new HashMap<>(Map.of("status","defined, exploded, released, inprocess, finished, annulled, cancelled, reactivated, productionreviewed, reviewed"))
                ),
                ignoreFields
        );
    }

}