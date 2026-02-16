package com.adasoft.pharmasuite.apips.api.recipe.service;

import com.adasoft.pharmasuite.apips.api.common.domain.PageResponse;
import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.recipe.domain.Recipe;
import com.adasoft.pharmasuite.apips.api.recipe.domain.RecipeFilterDTO;
import com.adasoft.pharmasuite.apips.api.recipe.domain.RecipeFilter;

import org.springframework.http.ResponseEntity;
import java.util.List;


public interface RecipeService {
    ResponseEntity<PageResponseOdata<Recipe>> getFilteredOData(final OdataPage odata);

    //TODO: limpiar
    List<Recipe> getAllRecipes(RecipeFilterDTO filter);
}
