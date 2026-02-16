package com.adasoft.pharmasuite.apips.api.workflow.service.impl;

import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.common.mapper.odata.OlingoQueryParser;
import com.adasoft.pharmasuite.apips.api.common.service.BaseService;
import com.adasoft.pharmasuite.apips.api.common.service.odata.GenericFilterVisitor;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.api.workflow.domain.Workflow;
import com.adasoft.pharmasuite.apips.api.workflow.mapper.RockwellWorkflowBindings;
import com.adasoft.pharmasuite.apips.api.workflow.mapper.WorkflowMapper;
import com.adasoft.pharmasuite.apips.api.workflow.service.WorkflowService;
import com.adasoft.pharmasuite.apips.api.workflow.util.WorkflowOrderByApplier;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.datasweep.compatibility.client.ProcessOrder;
import com.datasweep.compatibility.client.ProcessOrderFilter;
import com.datasweep.compatibility.client.ProcessOrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import lombok.SneakyThrows;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WorkflowServiceImpl extends BaseService implements WorkflowService {

    private final WorkflowMapper mapper = new WorkflowMapper();
    private final IFunctionsEx functions;
    private final ApplicationEdmProvider applicationEdmProvider;
    private final TaskExecutor  executor;

    public WorkflowServiceImpl(IFunctionsEx functions, Gson gsonMapper, ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, TaskExecutor executor, ApplicationEdmProvider applicationEdmProvider) {
        super(gsonMapper, objectMapper, memoryCacheService, jobQuartzService,webSocketHandler);
        this.functions = functions;
        this.executor = executor;
        this.applicationEdmProvider = applicationEdmProvider;
    }

    @SneakyThrows
    @Override
    public ResponseEntity<PageResponseOdata<Workflow>> getFilteredOData(final OdataPage odataPage) {
        try {
            String odataQuery = buildODataQuery(odataPage);
            ProcessOrderFilter processOrderFilter = functions.createProcessOrderFilter();
            var parser = new OlingoQueryParser(applicationEdmProvider, WorkflowEdmProvider.ES_WORKFLOWS);
            var uri = generateUriOdata(odataQuery, parser);

            var paging = generatePageOdata(odataQuery, parser);

            // 1) SIEMPRE: construir bindings de workflow (esto mete el baseline PROCESSING_TYPE = WORKFLOW)
            var bindings = RockwellWorkflowBindings.forWorkflow(functions);

            // 2) SOLO si hay $filter, aplicar la expresión OData
            if (uri.getFilterOption() != null && uri.getFilterOption().getExpression() != null) {
                uri.getFilterOption()
                        .getExpression()
                        .accept(new GenericFilterVisitor<>(bindings, processOrderFilter));
            }

            // 3) SIEMPRE: enganchar el subfiltro de items (workflow + lo que se haya añadido)
            RockwellWorkflowBindings.attachAccumulatedItemFilter(processOrderFilter);

            var orderApplier = new WorkflowOrderByApplier();
            orderApplier.applyToRockwell(processOrderFilter, uri.getOrderByOption());

            applyServerPaging(processOrderFilter, paging.getPageSize(), paging.getOffset());

            List<Workflow> data = buildAllWorkflowsOdata(processOrderFilter);
            Integer count = functions.getFilteredProcessOrderCount(processOrderFilter);

            return ResponseEntity.ok()
                    .headers(getHttpHeadersOdata(odataPage.getRequest()))
                    .body(new PageResponseOdata<>("",count.longValue(),data));

        } catch (Exception ex) {
            throw new IllegalArgumentException("Petición OData inválida: " + ex.getMessage(), ex);
        }
    }

    private List<Workflow> buildAllWorkflowsOdata(ProcessOrderFilter filter) {

        List<CompletableFuture<Workflow>> futures = new ArrayList<>();
        var filteredOrders = functions.getFilteredProcessOrders(filter);
        Semaphore semaphore = new Semaphore(semaphorePool, true); // fairness ON


        for (Object item : filteredOrders) {
            if (!(item instanceof ProcessOrder processOrder)) continue;

            List<?> orderItems = processOrder.getProcessOrderItems();
            if (orderItems.isEmpty() || orderItems.get(0) == null) continue;
            ProcessOrderItem mainItem = (ProcessOrderItem) orderItems.get(0);

            futures.add(
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            semaphore.acquire();
                            return mapper.toWorkflow(mainItem, filter.getServer());
                        } catch (Exception e) {
                            LogManagement.error(
                                    String.format("Error mapping workflow: %s", e.getMessage()), this
                            );
                            return null;
                        } finally {
                            semaphore.release();
                        }
                            }, executor)
                            .orTimeout(timeoutThread, TimeUnit.SECONDS)
                            .exceptionally(ex -> {
                                LogManagement.error("Task failed due to timeout or other exception: " + ex.getMessage(), WorkflowServiceImpl.class);
                                return null;
                            })

            );
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }


}
