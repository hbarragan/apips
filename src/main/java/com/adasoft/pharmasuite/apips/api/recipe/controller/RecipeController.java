package com.adasoft.pharmasuite.apips.api.recipe.controller;

import com.adasoft.pharmasuite.apips.api.common.controller.CommonController;
import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.recipe.domain.Recipe;
import com.adasoft.pharmasuite.apips.api.recipe.service.RecipeService;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value=ApiConstants.API_RECIPE, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = ApiConstants.TAG_RECIPE, description = ApiConstants.DESCRIPTION_RECIPE)
public class RecipeController extends CommonController {

    private final RecipeService recipeService;
    public RecipeController(RecipeService service) {
        this.recipeService = service;
    }

    @GetMapping(ApiConstants.PATH_PAGED)
    @Operation(
            summary = "Get filtered recipes paged",
            description =     "Return recipes. OData-like query parameters are supported for filtering and paging.<br/><br/>" +

                              "<b>Supported $filter fields (server-side):</b> " +
                              "<code>key</code>, <code>name</code>, <code>description</code>, <code>revision</code>, " +
                              "<code>creationDate</code>, <code>effectivityStartTime</code>, <code>effectivityEndTime</code>, " +
                              "<code>accessPrivilege</code>.<br/><br/>" +

                              "<b>Available fields in response (EDM):</b> " +
                              "<code>key</code>, <code>name</code>, <code>status</code>, <code>revision</code>, <code>description</code>, " +
                              "<code>creationDate</code>, <code>effectivityStartTime</code>, <code>effectivityEndTime</code>, " +
                              "<code>materialName</code>, <code>materialDescription</code>, <code>quantity</code>, <code>plannedQuantity</code>, " +
                              "<code>procedureName</code>, <code>accessPrivilege</code>.<br/><br/>" +

                              "<b>Examples:</b><br/><br/>" +
                              "<code>" + ApiConstants.API_RECIPE + "/paged?$top=20&$skip=0&$count=true</code><br/>" +
                              "<code>" + ApiConstants.API_RECIPE + "/paged?$top=20&$skip=0&$count=true&$filter=name%20eq%20%27Pan%27</code><br/>" +
                              "<code>" + ApiConstants.API_RECIPE + "/paged?$top=20&$skip=0&$count=true&$filter=accessPrivilege%20eq%20%27JC_Visualizacion%27</code>",
            operationId = "getAllRecipesPaged",
            tags = { ApiConstants.TAG_RECIPE}

    )
    public ResponseEntity<PageResponseOdata<Recipe>> getAllRecipesPaged(HttpServletRequest request,
       @RequestParam(name = "$filter", required = false) String filter,
       @RequestParam(name = "$orderby", required = false) String orderBy,
       @RequestParam(name = "$top", required = false) Integer top,
       @RequestParam(name = "$skip", required = false) Integer skip,
       @RequestParam(name = "$count", required = false) Boolean count) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);

        return recipeService.getFilteredOData(odataPage);
    }
}
