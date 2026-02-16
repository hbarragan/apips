package com.adasoft.pharmasuite.apips.api.recipe.service.impl;

import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.common.mapper.odata.OlingoQueryParser;
import com.adasoft.pharmasuite.apips.api.common.service.BaseService;
import com.adasoft.pharmasuite.apips.api.common.service.odata.GenericFilterVisitor;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.api.recipe.domain.Recipe;
import com.adasoft.pharmasuite.apips.api.recipe.domain.RecipeFilter;
import com.adasoft.pharmasuite.apips.api.recipe.domain.RecipeFilterDTO;
import com.adasoft.pharmasuite.apips.api.recipe.mapper.RecipeMapper;
import com.adasoft.pharmasuite.apips.api.recipe.mapper.RockwellRecipeBindings;
import com.adasoft.pharmasuite.apips.api.recipe.service.RecipeService;
import com.adasoft.pharmasuite.apips.api.recipe.util.RecipeOrderByApplier;
import com.adasoft.pharmasuite.apips.api.workflow.service.impl.WorkflowServiceImpl;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.core.utils.DateFormatUtil;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.datasweep.compatibility.client.MasterRecipe;
import com.datasweep.compatibility.client.MasterRecipeFilter;
import com.datasweep.compatibility.ui.Time;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import lombok.SneakyThrows;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RecipeServiceImpl extends BaseService implements RecipeService {
    private final RecipeMapper mapper = new RecipeMapper();
    private final IFunctionsEx functions;
    private final ApplicationEdmProvider applicationEdmProvider;
    private final TaskExecutor executor;
    public static final String BUILD_GET_ERROR_S = "buildGetAllRecipes: %s  ";


    public RecipeServiceImpl(IFunctionsEx functions, Gson gsonMapper, ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, TaskExecutor executor, ApplicationEdmProvider applicationEdmProvider) {
        super(gsonMapper, objectMapper, memoryCacheService, jobQuartzService, webSocketHandler);
        this.functions = functions;
        this.executor = executor;
        this.applicationEdmProvider = applicationEdmProvider;
    }


    @Override
    public List<Recipe> getAllRecipes(RecipeFilterDTO filter) {

        //TODO pasar al service siempre el DTO
        return buildGetAllRecipes(RecipeFilterDTO.toRecipeFilter(filter));
    }

    private List<Recipe> buildGetAllRecipes(RecipeFilter filter) {
        LogManagement.info("Init get filtered recipes ", this);
        MasterRecipeFilter masterRecipeFilter;
        try {
            masterRecipeFilter = applyFilterGetAllRecipes(filter);
        } catch (IllegalArgumentException e) {
            LogManagement.error("Error applying filters: " + e.getMessage(), this);
            return Collections.emptyList();
        }
        LogManagement.info("Init map filtered recipes ", this);
        List<CompletableFuture<Recipe>> futures = new ArrayList<>();
        Semaphore semaphore = new Semaphore(semaphorePool, true); // fairness ON

        for (Object item : functions.getFilteredMasterRecipes(masterRecipeFilter)) {
            LogManagement.info("Finish get filtered recipes ", this);
            if (item instanceof MasterRecipe masterRecipe) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                semaphore.acquire();
                                return  mapper.toDto(masterRecipe, masterRecipeFilter.getServer());
                            } catch (Exception e) {
                                LogManagement.error(String.format(BUILD_GET_ERROR_S, e.getMessage()), this);
                                return null;
                            } finally {
                                semaphore.release();
                            }
                        }, executor).orTimeout(timeoutThread, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            LogManagement.error("Task failed due to timeout or other exception: " + ex.getMessage(), WorkflowServiceImpl.class);
                            return null;
                        })
                );
            }
            }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream().map(CompletableFuture::join)
                .filter(Objects::nonNull).toList();
    }

    private List<Recipe> buildGetAllRecipes(MasterRecipeFilter filter) {
        List<CompletableFuture<Recipe>> futures = new ArrayList<>();
        LogManagement.info("Init get filtered recipes ", this);
        Semaphore semaphore = new Semaphore(semaphorePool, true);

        for (Object item : functions.getFilteredMasterRecipes(filter)) {
            LogManagement.info("Finish get filtered recipes ", this);
                if (item instanceof MasterRecipe masterRecipe) {
                    futures.add(CompletableFuture.supplyAsync(
                            () -> {
                                try {
                                    semaphore.acquire();
                                    return mapper.toDto(masterRecipe, filter.getServer());
                                } catch (Exception e) {
                                    LogManagement.error(String.format(BUILD_GET_ERROR_S, e.getMessage()), this);
                                    return null;
                                } finally {
                                    semaphore.release();
                                }
                            }, executor).orTimeout(timeoutThread, TimeUnit.SECONDS)
                            .exceptionally(ex -> {
                                LogManagement.error("Task failed due to timeout or other exception: " + ex.getMessage(), WorkflowServiceImpl.class);
                                return null;
                            })
                    );
                }
            }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream().map(CompletableFuture::join)
                .filter(Objects::nonNull).toList();

    }


    @SneakyThrows
    @Override
    public ResponseEntity<PageResponseOdata<Recipe>> getFilteredOData(final OdataPage odata) {
        try {
            String odataQuery=buildODataQuery(odata);
            com.datasweep.compatibility.client.MasterRecipeFilter masterRecipeFilter = functions.createMasterRecipeFilter();
            var parser      = new OlingoQueryParser(applicationEdmProvider, RecipeEdmProvider.ES_RECIPES);
            var uri         = generateUriOdata(odataQuery,parser);

            var paging = generatePageOdata(odataQuery, parser);

            if (uri.getFilterOption() != null && uri.getFilterOption().getExpression() != null) {
                var bindings = RockwellRecipeBindings.forRecipe(functions);
                try {
                    uri.getFilterOption().getExpression().accept(new GenericFilterVisitor<>(bindings, masterRecipeFilter));
                } finally {
                    RockwellRecipeBindings.finalizeAndClear();
                }
            }

            var orderApplier = new RecipeOrderByApplier();
            orderApplier.applyToRockwell(masterRecipeFilter, uri.getOrderByOption());

            applyServerPaging(masterRecipeFilter, paging.getPageSize(), paging.getOffset());
            List<Recipe> data = buildGetAllRecipes(masterRecipeFilter);
            long count =functions.getFilteredMasterRecipeCount(masterRecipeFilter);
            return ResponseEntity.ok().body(new PageResponseOdata<>("",count,data));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Petición OData inválida: " + ex.getMessage(), ex);
        }
    }


    private MasterRecipeFilter applyFilterGetAllRecipes(RecipeFilter filter) {
        MasterRecipeFilter masterRecipeFilter = functions.createMasterRecipeFilter();
        Calendar calendar = Calendar.getInstance();

        if (filter.getName() != null){
            masterRecipeFilter.forNameContaining(filter.getName());
        }

        if (filter.getStatus() != null) {
            for (String status : filter.getStatus()) {
                masterRecipeFilter.forCurrentStateEqualTo(status);
            }
        }
        if (filter.getInitCreationDate() != null && filter.getFinishCreationDate() != null) {
            masterRecipeFilter.forCreationTimeGreaterThanOrEqualTo(DateFormatUtil.convertToSqlTime(filter.getInitCreationDate()));
            masterRecipeFilter.forCreationTimeLessThan(DateFormatUtil.convertToSqlTime(filter.getFinishCreationDate()));
        } else if (filter.getLastMonthsInterval() != null) {
            LogManagement.info("apply cache filter :" + filter.getLastMonthsInterval(), this);
            calendar.add(Calendar.MONTH, -filter.getLastMonthsInterval());
            calendar= DateFormatUtil.truncateToDay(calendar);
            Time monthAgo = new Time(calendar);
            masterRecipeFilter.forCreationTimeGreaterThanOrEqualTo(monthAgo);
        } else {
            calendar.add(Calendar.MONTH, -monthInterval);
            LogManagement.info("apply cache application ``cache.custom.recipe.all`` :" + timeCache, this);
            calendar= DateFormatUtil.truncateToDay(calendar);
            Time monthAgo = new Time(calendar);
            masterRecipeFilter.forCreationTimeGreaterThanOrEqualTo(monthAgo);
        }
        setAccessPrivilegeOrReturnEmptyData(filter.getAccessPrivilege(), masterRecipeFilter, functions);
        return masterRecipeFilter;
    }


}