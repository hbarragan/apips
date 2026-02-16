package com.adasoft.pharmasuite.apips.api.batch.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.HelperSchema;
import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterCache;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = false)
@Data
@Schema(description = "Filter parameters for Batch queries.")
public class BatchFilter extends BaseFilterCache {

    @Parameter(
            name = "orderAssociateType",
            in = ParameterIn.QUERY,
            description = "Filter by state. [OUTPUT, INPUT ...]",
            explode = Explode.TRUE,
            schema = @Schema(type = "array", allowableValues = {""})
    )
    private List<String> orderAssociateType;

    @Parameter(
            name = "orderAssociateSubtype",
            in = ParameterIn.QUERY,
            description = "Filter by state. [PRODUCTION_OF_OUTPUT_MATERIAL, BATCH_GENERATION ...]",
            explode = Explode.TRUE,
            schema = @Schema(type = "array", allowableValues = {""})
    )
    private List<String> orderAssociateSubtype;

    @Override
    public Map<String, Object> getSchemaSwagger(Collection<String> ignoreFields) {
        return HelperSchema.minimalInputSchema(
                BatchFilter.class,
                buildBaseMapSchema(new HashMap<>(Map.of(
                        "orderAssociateType", "[OUTPUT, INPUT ...]",
                        "orderAssociateSubtype", "[PRODUCTION_OF_OUTPUT_MATERIAL, BATCH_GENERATION ...]"
                ))),
                ignoreFields
        );
    }
}
