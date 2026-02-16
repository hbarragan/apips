package com.adasoft.pharmasuite.apips.api.batch.service.impl;

import com.adasoft.pharmasuite.apips.api.batch.domain.*;
import com.adasoft.pharmasuite.apips.api.batch.mapper.BatchMapper;
import com.adasoft.pharmasuite.apips.api.batch.mapper.RockwellBatchBindings;
import com.adasoft.pharmasuite.apips.api.batch.mapper.RockwellTransactionHistoryBindings;
import com.adasoft.pharmasuite.apips.api.batch.mapper.SublotMapper;
import com.adasoft.pharmasuite.apips.api.batch.mapper.TransactionHistoryMapper;
import com.adasoft.pharmasuite.apips.api.batch.service.BatchService;
import com.adasoft.pharmasuite.apips.api.batch.util.BatchOrderByApplier;
import com.adasoft.pharmasuite.apips.api.batch.util.TransactionHistoryOrderByApplier;
import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.common.mapper.odata.OlingoQueryParser;
import com.adasoft.pharmasuite.apips.api.common.service.BaseService;
import com.adasoft.pharmasuite.apips.api.common.service.odata.GenericFilterVisitor;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.api.workflow.service.impl.WorkflowServiceImpl;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import com.adasoft.pharmasuite.apips.core.utils.DateFormatUtil;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.datasweep.compatibility.client.DatasweepException;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.services.inventory.ifc.ITransactionHistoryObject;
import com.rockwell.mes.services.inventory.ifc.TransactionHistoryObject;
import com.rockwell.mes.services.inventory.ifc.TransactionSubtype;
import com.rockwell.mes.services.inventory.ifc.TransactionType;
import com.rockwell.mes.services.inventory.impl.SublotService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class BatchServiceImpl extends BaseService implements BatchService {

    public static final String BUILD_GET_ALL_BATCH_S = "buildGetAllBatch: %s  ";
    public static final String BUILD_GET_ALL_TRANSACTION_HISTORY_S = "buildGetAllTransactionHistory: %s  ";
    private final BatchMapper mapper = new BatchMapper();
    private final SublotMapper sublotMapper = new SublotMapper();
    private final TransactionHistoryMapper transactionHistoryMapper = new TransactionHistoryMapper();
    private final IFunctionsEx functions;
    private final SublotService sublotService;
    private final ApplicationEdmProvider applicationEdmProvider;
    private final TaskExecutor executor;


    public BatchServiceImpl(IFunctionsEx functions, Gson gsonMapper, ObjectMapper objectMapper, MemoryCacheService memoryCacheService, SublotService sublotService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, TaskExecutor executor, ApplicationEdmProvider applicationEdmProvider) {
        super(gsonMapper, objectMapper, memoryCacheService, jobQuartzService, webSocketHandler);
        this.functions = functions;
        this.sublotService = sublotService;
        this.executor = executor;
        this.applicationEdmProvider = applicationEdmProvider;
    }

    public List<Batch> getAllBatch(BatchFilterDTO filter){
        return buildGetAllBatch(BatchFilterDTO.toBatchFilter(filter));
    }


    @Override
    public ResponseEntity<List<Sublot>> getFilteredSubLot(SublotFilter filter) {
        try {
            return ResponseEntity.ok(buildGetAllSublot(filter));
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.singletonList((Sublot) buildError(Sublot.builder().build(), e, "getAllSublot")));
        }
    }

    @Override
    public ResponseEntity<List<TransactionHistory>> getFilteredTransactionHistory(TransactionHistoryFilter filter) {
        try {
            return ResponseEntity.ok(buildGetAllTransactionHistory(filter));
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.singletonList((TransactionHistory) buildError(TransactionHistory.builder().build(), e, "getAllTransactionHistory")));
        }
    }


    @Override
    public ResponseEntity<PageResponseOdata<Batch>> getFilteredBatchOData(final OdataPage odataPage) {
        try {
            String odataQuery=buildODataQuery(odataPage);
            com.datasweep.compatibility.client.BatchFilter rf = functions.createBatchFilter();
            var parser      = new OlingoQueryParser(applicationEdmProvider, BatchEdmProvider.ES_BATCH);
            var uri         = generateUriOdata(odataQuery,parser);

            var paging = generatePageOdata(odataQuery, parser);

            if (uri.getFilterOption() != null && uri.getFilterOption().getExpression() != null) {
                var bindings = RockwellBatchBindings.forBatch(
                        functions,
                        ApiConstants.DEFAULT_QUANTITY_UOM,
                        ApiConstants.DEFAULT_QUANTITY_UOM
                );
                uri.getFilterOption().getExpression().accept(new GenericFilterVisitor<>(bindings, rf));
            }

            var orderApplier = new BatchOrderByApplier();
            orderApplier.applyToRockwell(rf, uri.getOrderByOption());

            applyServerPaging(rf, paging.getPageSize(), paging.getOffset());

            List<Batch> data = buildGetAllBatch(rf,rf.getServer());
            Integer count =functions.getFilteredBatchCount(rf);
            return ResponseEntity.ok().headers(getHttpHeadersOdata(odataPage.getRequest())).body(new PageResponseOdata<>("",count.longValue(),data));

        } catch (Exception ex) {
            throw new IllegalArgumentException("Petición OData inválida: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ResponseEntity<PageResponseOdata<TransactionHistory>> getFilteredTransactionHistoryOData(OdataPage odataPage) {
        try {
            String odataQuery=buildODataQuery(odataPage);
            com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter filterRockwell = new com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter();

            var parser      = new OlingoQueryParser(applicationEdmProvider, TransactionHistoryEdmProvider.ES_TRANSACTION_HISTORY);
            var uri         = generateUriOdata(odataQuery,parser);
            var paging      = generatePageOdata(odataQuery, parser);

            if (uri.getFilterOption() != null && uri.getFilterOption().getExpression() != null) {
                var bindings = RockwellTransactionHistoryBindings.forTransactionHistory();
                uri.getFilterOption().getExpression().accept(new GenericFilterVisitor<>(bindings, filterRockwell));
            }
            long count =filterRockwell.getCount();
            var orderApplier = new TransactionHistoryOrderByApplier();
            orderApplier.applyToRockwell(filterRockwell, uri.getOrderByOption());

            applyServerPaging(filterRockwell, paging.getPageSize(), paging.getOffset());

            List<TransactionHistory> data = buildGetAllTransactionHistory(filterRockwell);
            return ResponseEntity.ok().headers(getHttpHeadersOdata(odataPage.getRequest())).body(new PageResponseOdata<>("",count,data));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Petición OData inválida: " + ex.getMessage(), ex);
        }
    }

    private com.datasweep.compatibility.client.BatchFilter applyFilter(BatchFilter filter) {
        try {
            com.datasweep.compatibility.client.BatchFilter batchFilter = functions.createBatchFilter();
            Calendar calendar = Calendar.getInstance();

            if (filter.getInitCreationDate() != null || filter.getFinishCreationDate() != null) {
                if(filter.getInitCreationDate() != null && !filter.getInitCreationDate().isEmpty()) {
                    batchFilter.forCreationTimeGreaterThanOrEqualTo(DateFormatUtil.convertToSqlTime(filter.getInitCreationDate()));
                }
                if(filter.getFinishCreationDate()!=null && !filter.getFinishCreationDate().isEmpty()) {
                    batchFilter.forCreationTimeLessThan(DateFormatUtil.convertToSqlTime(filter.getFinishCreationDate()));
                }
                LogManagement.info(format(ADD_FILTER_CREATION_TIME_GREATER_THAN_OR_EQUAL_TO_S_FOR_CREATION_TIME_LESS_THAN, filter.getInitCreationDate(), filter.getFinishCreationDate()), this.getClass());
            } else if (filter.getLastMonthsInterval() != null) {
                LogManagement.info("apply cache filter :" + filter.getLastMonthsInterval(), this);
                calendar.add(Calendar.MONTH, -filter.getLastMonthsInterval());
                calendar = DateFormatUtil.truncateToDay(calendar);
                batchFilter.forCreationTimeGreaterThanOrEqualTo(new Time(calendar));
            }
            LogManagement.info(batchFilter.toString(), this);
            return batchFilter;
        } catch (Exception e) {
            LogManagement.error(format(ERROR_WHILE_PARSING_CREATION_DATES_INIT_CREATION_DATE_AND_FINISH_CREATION_DATE, filter.getInitCreationDate(), filter.getFinishCreationDate(), e), this);
            throw new RuntimeException(ERROR_WHILE_PARSING_CREATION_DATES, e);

        }
    }


    private com.datasweep.compatibility.client.SublotFilter applyFilterSublot(SublotFilter filter) {
            com.datasweep.compatibility.client.SublotFilter sublotFilter = functions.createSublotFilter();
            if(filter.getBatchName()!=null) {
                sublotFilter.forBatchNameEqualTo(filter.getBatchName());
            }
            if(filter.getBatchKey()!=null) {
                sublotFilter.forBatchKeyEqualTo(filter.getBatchKey());
            }
            LogManagement.info(sublotFilter.toString(), this);
            return sublotFilter;
    }

    private com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter applyFilterTransactionHistory(TransactionHistoryFilter filter) throws DatasweepException {
        com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter filterRockwell = new com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter();
        filterRockwell.forBatchIdentifierNewEqualTo(filter.getBatchName());

        // --- Filtro por TYPE (X_transactionType) ---
        if (filter.getOrderAssociateType() != null && !filter.getOrderAssociateType().isEmpty()) {
            long[] typeValues = filter.getOrderAssociateType().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> TransactionType.valueOf(s.toUpperCase(Locale.ROOT)))
                    .mapToLong(TransactionType::getValue)
                    .toArray();

            if (typeValues.length > 0) {
                // IN sobre la columna X_transactionType
                filterRockwell.forColumnNameIn(TransactionHistoryObject.COL_NAME_TRANSACTION_TYPE, typeValues);
            }
        }

        // --- Filtro por SUBTYPE (X_transactionSubtype) ---
        if (filter.getOrderAssociateSubtype() != null && !filter.getOrderAssociateSubtype().isEmpty()) {
            long[] subtypeValues = filter.getOrderAssociateSubtype().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> TransactionSubtype.valueOf(s.toUpperCase(Locale.ROOT)))
                    .mapToLong(TransactionSubtype::getValue)
                    .toArray();

            if (subtypeValues.length > 0) {
                // IN sobre la columna X_transactionSubtype
                filterRockwell.forColumnNameIn(TransactionHistoryObject.COL_NAME_TRANSACTION_SUBTYPE, subtypeValues);
            }
        }

        return filterRockwell;
    }

    private List<Sublot> buildGetAllSublot(SublotFilter filter) {
        com.datasweep.compatibility.client.SublotFilter filterRockwell =
                applyFilterSublot(filter);
        List<CompletableFuture<Sublot>> futures = new ArrayList<>();
        Semaphore semaphore = new Semaphore(semaphorePool, true); // fairness ON

        for (Object item : functions.getFilteredSublots(filterRockwell)) {
            if (item instanceof com.datasweep.compatibility.client.Sublot sublot) {
                futures.add(CompletableFuture.supplyAsync(
                                () -> {
                                    try {
                                        semaphore.acquire();
                                        return sublotMapper.toDto(sublot, sublotService);

                                    } catch (Exception e) {
                                        LogManagement.error(
                                                String.format(BUILD_GET_ALL_BATCH_S, e.getMessage()),
                                                this
                                        );
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

    private List<TransactionHistory> buildGetAllTransactionHistory(TransactionHistoryFilter filter) throws DatasweepException {
        return buildGetAllTransactionHistory(applyFilterTransactionHistory(filter));
    }

    private List<TransactionHistory> buildGetAllTransactionHistory(com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter filterRockwell) {
        Semaphore semaphore = new Semaphore(semaphorePool, true); // fairness ON

        List<ITransactionHistoryObject> list =
                com.rockwell.mes.services.inventory.impl.TransactionHistoryFilter
                        .getFilteredTransactionHistoryObjects(filterRockwell);

        List<CompletableFuture<TransactionHistory>> futures = new ArrayList<>(list.size());

        for (Object item : list) {
            if (item instanceof ITransactionHistoryObject txObj) {
                futures.add(mapAsync(txObj, semaphore));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    private CompletableFuture<TransactionHistory> mapAsync(ITransactionHistoryObject txObj, Semaphore semaphore) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        return transactionHistoryMapper.toDto(txObj);
                    } catch (Exception e) {
                        LogManagement.error(
                                String.format(BUILD_GET_ALL_TRANSACTION_HISTORY_S, e.getMessage()),
                                this
                        );
                        return null;
                    } finally {
                        semaphore.release();
                    }
                }, executor)
                .orTimeout(timeoutThread, TimeUnit.SECONDS).exceptionally(ex -> {
                    LogManagement.error("Task failed due to timeout or other exception: " + ex.getMessage(), WorkflowServiceImpl.class);
                    return null;
                });
    }


    private List<Batch> buildGetAllBatch(BatchFilter filter) {

        com.datasweep.compatibility.client.BatchFilter filterRockwell =
                applyFilter(filter);
        List<CompletableFuture<Batch>> futures = new ArrayList<>();

        Semaphore semaphore = new Semaphore(semaphorePool, true); // fairness ON
        for (Object item : functions.getFilteredBatches(filterRockwell)) {
            if (item instanceof com.datasweep.compatibility.client.Batch itemBatch) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                            try {
                                semaphore.acquire();
                                return mapper.toDto(
                                        itemBatch,
                                        sublotService,
                                        filter.getOrderAssociateType(),
                                        filter.getOrderAssociateSubtype(),
                                        filterRockwell.getServer()
                                );
                            } catch (Exception e) {
                                LogManagement.error(
                                        String.format(BUILD_GET_ALL_BATCH_S, e.getMessage()),
                                        this
                                );
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


    private List<Batch> buildGetAllBatch(com.datasweep.compatibility.client.BatchFilter rf, ServerImpl server) {

        List<CompletableFuture<Batch>> futures = new ArrayList<>();
        Semaphore semaphore = new Semaphore(semaphorePool, true); // fairness ON

        for (Object item : functions.getFilteredBatches(rf)) {
            if (item instanceof com.datasweep.compatibility.client.Batch b) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                            try {
                                semaphore.acquire();
                                return mapper.toDto(b, sublotService, null, null, server);
                            } catch (Exception e) {
                                LogManagement.error(
                                        String.format(BUILD_GET_ALL_BATCH_S, e.getMessage()),
                                        this
                                );
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

}
