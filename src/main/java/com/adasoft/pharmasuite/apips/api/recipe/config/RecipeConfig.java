package com.adasoft.pharmasuite.apips.api.recipe.config;

import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.api.recipe.service.RecipeService;
import com.adasoft.pharmasuite.apips.api.recipe.service.impl.RecipeServiceImpl;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.services.recipe.ifc.IMESRecipeService;
import com.rockwell.mes.services.recipe.impl.MESRecipeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;

@Configuration
@DependsOn("apiConfig")
public class RecipeConfig {

    @Bean
    public IMESRecipeService recipeServicePS(){ return new MESRecipeService();}

    @Bean
    public RecipeService recipeService(IFunctionsEx functions, Gson gsonMapper , ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, TaskExecutor executor, ApplicationEdmProvider applicationEdmProvider) {
        return new RecipeServiceImpl(functions,  gsonMapper ,objectMapper, memoryCacheService, jobQuartzService, webSocketHandler, executor, applicationEdmProvider);
    }
}
