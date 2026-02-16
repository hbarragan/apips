package com.adasoft.pharmasuite.apips.api.recipe.domain;


import com.adasoft.pharmasuite.apips.api.common.domain.HelperSchema;
import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterCache;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = false)
@Data
@Schema(description = "Filter parameters for recipe queries.")
public class RecipeFilter extends BaseFilterCache {

    @Schema(
            description = "Name filter'",
            example = "name",
            hidden = true
    )
    @Parameter(
            name = "Name",
            in = ParameterIn.QUERY,
            example = "",
            description = "Filter by name"
    )
    private String name;

    @Parameter(
            name = "status",
            in = ParameterIn.QUERY,
            description = "Filter by state. Allowed values: Valid, Edit, Verification, Scheduled, Archive",
            example = "Valid",
            explode = Explode.TRUE,
            schema = @Schema(type = "array", allowableValues = {"Valid", "Edit", "Verification", "Scheduled", "Archive"})
    )
    private List<String> status;

    @Parameter(
            name = "accessPrivilege",
            in = ParameterIn.QUERY,
            example = "JC_Visualizacion",
            description = "Filter by access privilege"
    )
    private String accessPrivilege;

    @Override
    public Map<String, Object> getSchemaSwagger(Collection<String> ignoreFields) {
        return HelperSchema.minimalInputSchema(
                this.getClass(),
                buildBaseMapSchema(new HashMap<>()),
                ignoreFields
        );
    }
}
