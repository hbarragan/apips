package com.adasoft.pharmasuite.apips.api.recipe.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterCache;
import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

@Getter
@Setter
public class RecipeFilterDTO extends BaseFilterDTO {


    @ToolParam(
            description = "Name or partial name used to filter results. " +
                    "If not specified, results will not be filtered by name.",
            required = false
    )
    private String name;

    @ToolParam(
            description = "List of status values used to filter results. " +
                    "Only records whose status matches one of the provided values will be returned. " +
                    "Example values: 'valid', 'edit', 'verification', 'scheduled', 'archive'. " +
                    "If not specified, all statuses will be included.",
            required = false
    )
    private List<String> status;

    @ToolParam(
            description = "Access privilege required to retrieve the results. " +
                    "Used to validate that the caller has sufficient permissions before returning data. " +
                    "If not specified, the default access control rules will be applied.",
            required = false
    )
    private String accessPrivilege;

    public static RecipeFilter toRecipeFilter(RecipeFilterDTO dto) {
        if (dto == null) {
            return null;
        }

        RecipeFilter filterRecipe = new RecipeFilter();
        filterRecipe.setName(dto.getName()!= null ? dto.getName() : null);
        filterRecipe.setStatus(dto.getName()!= null ? dto.getStatus() : null);
        filterRecipe.setAccessPrivilege(dto.getName()!= null ? dto.getAccessPrivilege() : null);

        filterRecipe.setLastMonthsInterval(dto != null ? dto.getLastMonthsInterval() : null);
        filterRecipe.setInitCreationDate(dto != null ? dto.getInitCreationDate() : null);
        filterRecipe.setFinishCreationDate(dto != null ? dto.getFinishCreationDate() : null);

        return filterRecipe;
    }
}
