package com.adasoft.pharmasuite.apips.api.exception.service.impl;

import com.adasoft.pharmasuite.apips.api.exception.domain.ExceptionFilter;
import com.adasoft.pharmasuite.apips.api.recipe.domain.Recipe;
import com.adasoft.pharmasuite.apips.api.workflow.service.impl.WorkflowServiceImpl;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.api.common.service.BaseService;
import com.adasoft.pharmasuite.apips.api.exception.domain.ExceptionPS;
import com.adasoft.pharmasuite.apips.api.exception.mapper.ExceptionMapper;
import com.adasoft.pharmasuite.apips.api.exception.service.ExceptionService;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.datasweep.compatibility.client.DatasweepException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.deviation.ifc.IExceptionRecordingService;
import com.rockwell.mes.commons.deviation.ifc.exceptionrecording.IMESExceptionRecord;
import com.rockwell.mes.commons.deviation.ifc.exceptionrecording.IMESExceptionRecordFilter;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ExceptionServiceImpl extends BaseService implements ExceptionService {
    private final ExceptionMapper mapper = new ExceptionMapper();
    private final IExceptionRecordingService service;
    private final TaskExecutor executor;
    public static final String BUILD_GET_ERROR_S = "buildExceptions: %s  ";


    public ExceptionServiceImpl(Gson gsonMapper, ObjectMapper objectMapper, IExceptionRecordingService service, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, TaskExecutor executor) {
        super(gsonMapper, objectMapper, memoryCacheService, jobQuartzService, webSocketHandler);
        this.service = service;
        this.executor = executor;
    }

    @Override
    public ResponseEntity<List<ExceptionPS>> getAllException(ExceptionFilter filter) {
        try {
            Object cache = getCache(filter, timeCache);
            if (cache != null) return ResponseEntity.ok((List<ExceptionPS>) cache);
            List<ExceptionPS> items = buildExceptions(filter);
            putCache(filter, items, filter.getTimeCache() != null ? filter.getTimeCache() : timeCache);
            ResponseEntity<List<ExceptionPS>> response = ResponseEntity.ok(items);
            checkJobQuartz(filter);
            return response;
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.singletonList((ExceptionPS) buildError(Recipe.builder().build(), e, "getAllProcessOrders")));
        }
    }



    private List<ExceptionPS> buildExceptions(ExceptionFilter filterApi) throws DatasweepException {
        List<CompletableFuture<ExceptionPS>> futures = new ArrayList<>();
        IMESExceptionRecordFilter filter = service.createExceptionRecordFilter();
        List<IMESExceptionRecord> listExceptions = filter.getFilteredObjects();

        if (filterApi != null) {
            if(filterApi.getRiskLevel()!=null){
                filter.forRiskClassEqualTo(getRisk(filterApi.getRiskLevel()));
            }
            if(filterApi.getState()!=null){
                filter.forStatusEqualTo(getStateException(filterApi.getState()));
            }
        }

        Semaphore semaphore = new Semaphore(semaphorePool, true);
        for (IMESExceptionRecord item : listExceptions) {

            futures.add(CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            semaphore.acquire();
                            return mapper.toDto(item);
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
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream().map(CompletableFuture::join)
                .filter(Objects::nonNull).toList();
    }

    private Long getStateException(String state){
        return switch (state.toLowerCase()) {
            case "status" -> 0L;
            case "open" -> 1L;
            case "na" -> 2L;
            case "closed" -> 3L;
            case "to_be_closed" -> 4L;
            default -> Long.valueOf(state);
        };
    }

    private Long getRisk(String riskLevel) {
        return switch (riskLevel.toLowerCase()) {
            case "none" -> 0L;
            case "low" -> 1L;
            case "medium" -> 2L;
            case "high" -> 3L;
            default -> Long.valueOf(riskLevel);
        };
    }
}
