package com.adasoft.pharmasuite.apips.api.exception.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.HelperSchema;
import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterCache;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = false)
@Data
@Schema(description = "Filter parameters for exception queries.")
public class ExceptionFilter extends BaseFilterCache {
    @Parameter(
            name = "status",
            in = ParameterIn.QUERY,
            description = "Filter by state. Allowed values: open, closed , to_be_closed, na ( not apply )",
            explode = Explode.TRUE,
            schema = @Schema(type = "array", allowableValues = {
                    "open",
                    "closed",
                    "to_be_closed",
                    "na"})
    )
    private String state;
    @Parameter(
            name = "riskLevel",
            in = ParameterIn.QUERY,
            description = "Filter by risk level. Allowed values: none, low, lowWithMandatoryComment, medium, mediumWithMandatoryComment, high, highWithMandatoryComment",
            explode = Explode.TRUE,
            schema = @Schema(
                    type = "array",
                    allowableValues = {
                            "none",
                            "low",
                            "lowWithMandatoryComment",
                            "medium",
                            "mediumWithMandatoryComment",
                            "high",
                            "highWithMandatoryComment"
                    }
            )
    )

    private String riskLevel;

    @Override
    public Map<String, Object> getSchemaSwagger(Collection<String> ignoreFields) {
        return HelperSchema.minimalInputSchema(
                OrderFilter.class,
                buildBaseMapSchema(new HashMap<>(Map.of(
                        "status","open, closed , to_be_closed, na",
                        "riskLevel","none, low, lowWithMandatoryComment, medium, mediumWithMandatoryComment, high, highWithMandatoryComment"
                ))),
                ignoreFields
        );
    }
}