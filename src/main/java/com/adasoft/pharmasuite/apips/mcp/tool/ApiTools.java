package com.adasoft.pharmasuite.apips.mcp.tool;

import com.adasoft.pharmasuite.apips.api.batch.domain.Batch;
import com.adasoft.pharmasuite.apips.api.batch.domain.BatchFilterDTO;
import com.adasoft.pharmasuite.apips.api.batch.service.BatchService;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilterDTO;
import com.adasoft.pharmasuite.apips.api.order.domain.ProcessOrder;
import com.adasoft.pharmasuite.apips.api.order.service.ProcessOrdersService;
import com.adasoft.pharmasuite.apips.api.recipe.domain.Recipe;
import com.adasoft.pharmasuite.apips.api.recipe.domain.RecipeFilterDTO;
import com.adasoft.pharmasuite.apips.api.recipe.service.RecipeService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApiTools {

    private final RecipeService recipeService;
    private final ProcessOrdersService processOrdersService;
    private final BatchService batchService;

    public ApiTools(RecipeService recipeService, ProcessOrdersService processOrdersService, BatchService batchService) {
        this.recipeService = recipeService;
        this.processOrdersService = processOrdersService;
        this.batchService = batchService;
    }

    @Tool(
            name = "PS API - Service: getAllRecipes",
            description = "Retrieve recipes using a flexible filter object. " +
                    "This tool supports filtering by recipe name, status, creation date range, " +
                    "and access privileges. All filter fields are optional and can be combined. " +
                    "If no filters are provided, all accessible recipes will be returned.")
    public List<Recipe> getAllRecipes(RecipeFilterDTO filter) {
        return recipeService.getAllRecipes(filter);
    }


    @Tool(
            name = "PS API - Service : getAllProcessOrders",
            description = "Retrieve orders using multiple optional filtering criteria. " +
                    "This tool allows searching orders by name, status, ERP date range, " +
                    "creation date range, and access privileges. " +
                    "Additional base filters such as lastMonthsInterval, initCreationDate, " +
                    "and finishCreationDate are also supported. " +
                    "An optional flag enables retrieval of extended order details, " +
                    "including consumed quantities, work center, and location information. " +
                    "All filters are optional and can be combined. " +
                    "If no filters are provided, the result will include all accessible orders " +
                    "according to the caller's permissions.")
    public List<ProcessOrder> getAllProcessOrders(OrderFilterDTO filter) {
        return processOrdersService.getAllProcessOrders(filter);
    }

    @Tool(name= "PS API - Service : getAllBatch", description =
            "Retrieve batches using optional filters. " +
            "Supports filtering by order association type/subtype (e.g., OUTPUT/INPUT and PRODUCTION_OF_OUTPUT_MATERIAL/BATCH_GENERATION) " +
            "and by creation time window (initCreationDate/finishCreationDate in ISO 8601 UTC). " +
            "If no creation dates are provided, lastMonthsInterval is applied (default from configuration, typically 5 months) to optimize results."
            )
    public List<Batch> getAllBatch(BatchFilterDTO filter){
       return batchService.getAllBatch(filter);
    }
}
