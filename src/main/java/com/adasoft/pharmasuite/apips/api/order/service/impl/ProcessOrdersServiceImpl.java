package com.adasoft.pharmasuite.apips.api.order.service.impl;

import com.adasoft.pharmasuite.apips.api.common.domain.PageResponse;
import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.common.mapper.odata.OlingoQueryParser;
import com.adasoft.pharmasuite.apips.api.common.service.BaseService;
import com.adasoft.pharmasuite.apips.api.common.service.odata.GenericFilterVisitor;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderDetailFilter;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilter;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilterDTO;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilterPaged;
import com.adasoft.pharmasuite.apips.api.order.domain.ProcessOrder;
import com.adasoft.pharmasuite.apips.api.order.domain.ProcessOrderDetail;
import com.adasoft.pharmasuite.apips.api.order.mapper.ProcessOrderDetailMapper;
import com.adasoft.pharmasuite.apips.api.order.mapper.ProcessOrderMapper;
import com.adasoft.pharmasuite.apips.api.order.mapper.RockwellProcessOrderBindings;
import com.adasoft.pharmasuite.apips.api.order.service.ProcessOrdersService;
import com.adasoft.pharmasuite.apips.api.order.util.ProcessOrderOrderByApplier;
import com.adasoft.pharmasuite.apips.api.workflow.service.impl.WorkflowServiceImpl;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.DateFormatUtil;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.datasweep.compatibility.client.ProcessOrderFilter;
import com.datasweep.compatibility.client.ProcessOrderItem;
import com.datasweep.compatibility.client.ProcessOrderItemFilter;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.datasweep.plantops.common.constants.filtering.IFilterComparisonOperators;
import com.datasweep.plantops.common.constants.filtering.IKeyedFilterAttributes;
import com.datasweep.plantops.common.constants.filtering.IProcessOrderItemFilterAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.services.commons.ifc.order.IOrderStepOutputService;
import com.rockwell.mes.services.order.ifc.OrderUtils;
import com.rockwell.mes.services.s88.ifc.IS88ExecutionService;
import com.rockwell.mes.services.s88.ifc.IS88OrderAppendService;
import com.rockwell.mes.services.s88.ifc.S88ProcessingType;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ProcessOrdersServiceImpl extends BaseService implements ProcessOrdersService {

    private final ProcessOrderMapper mapper = new ProcessOrderMapper();
    private final ProcessOrderDetailMapper mapperDetail = new ProcessOrderDetailMapper();
    private final IFunctionsEx functions;
    private final IOrderStepOutputService orderStepOutputService;
    private final IS88OrderAppendService orderAppendService;
    private final IS88ExecutionService executionService;
    private final ApplicationEdmProvider applicationEdmProvider;


    private final TaskExecutor executor;
    public static final String BUILD_GET_ERROR_S = "buildAllOrders: %s  ";


    public ProcessOrdersServiceImpl(IFunctionsEx functions, IOrderStepOutputService orderStepOutputService, IS88OrderAppendService orderAppendService, IS88ExecutionService executionService, Gson gsonMapper, ObjectMapper objectMapper,
            MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler,
            TaskExecutor executor, ApplicationEdmProvider applicationEdmProvider) {
        super(gsonMapper, objectMapper, memoryCacheService, jobQuartzService, webSocketHandler);
        this.functions = functions;
        this.orderStepOutputService = orderStepOutputService;
        this.executor = executor;
        this.orderAppendService = orderAppendService;
        this.executionService = executionService;
        this.applicationEdmProvider = applicationEdmProvider;
    }

    @Override
    public ResponseEntity<List<ProcessOrder>> getAllProcessOrders(OrderFilter filter) throws RuntimeException {
        try {
            List<ProcessOrder> items = getOrStaleThenRefresh(
                    filter, timeCache,
                    () -> {
                        try {
                            return buildAllOrders(filter);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
            checkJobQuartz(filter);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.singletonList((ProcessOrder) buildError(ProcessOrder.builder().build(), e, "getAllProcessOrders")));
        }
    }

    @Override
    public List<ProcessOrder> getAllProcessOrders(OrderFilterDTO filter) {
        return buildAllOrders(OrderFilterDTO.toOrderFilter(filter));
    }

    @Override
    public ResponseEntity<ProcessOrderDetail> getProcessOrderDetail(OrderDetailFilter filter) {
        ProcessOrderItemFilter processOrderItemFilter=getFilters(filter);
        var vector = functions.getFilteredProcessOrderItems(processOrderItemFilter);
        for (Object item : vector) {
            if (item instanceof com.datasweep.compatibility.client.ProcessOrderItem poi) {
                return ResponseEntity.ok(mapperDetail.toProcessOrderDetail(poi,orderStepOutputService,orderAppendService,executionService));
            }
        }
        return ResponseEntity.ok(new ProcessOrderDetail());
    }



    @Override
    public ResponseEntity<PageResponse<ProcessOrder>> getAllProcessOrdersPaged(OrderFilterPaged filter) {
        try {
            PageResponse<ProcessOrder> items = buildAllOrdersPaged(filter);
            if(filter.isEnableCache()) {
                putCache(filter, items, filter.getTimeCache() != null ? filter.getTimeCache() : timeCache);
            }
            checkJobQuartz(filter);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            ProcessOrder po = (ProcessOrder) buildError(ProcessOrder.builder().build(),e,"getAllProcessOrders");
            return ResponseEntity.ok(new PageResponse<>(Collections.singletonList(po), 1, 0, 0) );
        }
    }

    @Override
    public ResponseEntity<PageResponseOdata<ProcessOrder>> getFilteredOData(final OdataPage odataPage) {
        try {
            String odataQuery = buildODataQuery(odataPage);
            ProcessOrderFilter processOrderFilter = functions.createProcessOrderFilter();
            ProcessOrderItemFilter itemFilter = functions.createProcessOrderItemFilter();

            var parser = new OlingoQueryParser(applicationEdmProvider, ProcessOrderEdmProvider.ES_PROCESS_ORDERS);
            var uri = generateUriOdata(odataQuery, parser);
            var paging = generatePageOdata(odataQuery, parser);

            if (uri.getFilterOption() != null && uri.getFilterOption().getExpression() != null) {
                var orderBindings = RockwellProcessOrderBindings.forProcessOrder(functions);
                var itemBindings = RockwellProcessOrderBindings.forProcessOrderItem(functions);
                
                uri.getFilterOption().getExpression().accept(new GenericFilterVisitor<>(orderBindings, processOrderFilter));
                uri.getFilterOption().getExpression().accept(new GenericFilterVisitor<>(itemBindings, itemFilter));
            }

            itemFilter.forUdaEqualTo(UdaConstant.PROCESSING_TYPE, S88ProcessingType.BATCH.getChoiceElement().getValue());

            processOrderFilter.addSearchForSubFilter(
                    IKeyedFilterAttributes.KEY,
                    IFilterComparisonOperators.IN,
                    IProcessOrderItemFilterAttributes.ORDER_KEY,
                    itemFilter
            );

            var orderApplier = new ProcessOrderOrderByApplier();
            orderApplier.applyToRockwell(processOrderFilter, uri.getOrderByOption());

            applyServerPaging(processOrderFilter, paging.getPageSize(), paging.getOffset());

            var vector = functions.getFilteredProcessOrders(processOrderFilter);
            int count = functions.getFilteredProcessOrderCount(processOrderFilter);

            List<ProcessOrder> data = new ArrayList<>();
            for (Object item : vector) {
                if (item instanceof com.datasweep.compatibility.client.ProcessOrder po) {
                    if (po.getProcessOrderItems().get(0) != null) {
                        ProcessOrderItem processOrderItem = (ProcessOrderItem) po.getProcessOrderItems().get(0);
                        if (!OrderUtils.isWorkflowOrder(processOrderItem)) {
                            ProcessOrder poi = mapper.toProcessOrder(processOrderItem, processOrderFilter.getServer(), orderStepOutputService, orderAppendService, executionService, false);
                            poi.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));
                            data.add(poi);
                        }
                    }
                }
            }

            return ResponseEntity.ok().headers(getHttpHeadersOdata(odataPage.getRequest()))
                    .body(new PageResponseOdata<>("", (long) count, data));

        } catch (Exception ex) {
            throw new IllegalArgumentException(buildErrorDataSweepException(ex),ex);
        }
    }

    private List<ProcessOrder> buildAllOrders(OrderFilter filter) {
        LogManagement.info("Init get filtered orders ", this);
        ProcessOrderFilter processOrderFilter = getFilters(filter);
        var vector = functions.getFilteredProcessOrders(processOrderFilter);
        LogManagement.info("Finish get filtered orders ", this);

        LogManagement.info("Init map filtered orders ", this);
        List<CompletableFuture<ProcessOrder>> futures = new ArrayList<>();
        Semaphore semaphore = new Semaphore(semaphorePool, true);

        for (Object item : vector) {
            if (item instanceof com.datasweep.compatibility.client.ProcessOrder po) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                semaphore.acquire();
                                return buildProcessOrder(po, processOrderFilter.getServer(), filter.isAll());
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

    private PageResponse<ProcessOrder> buildAllOrdersPaged(OrderFilterPaged filter) {
        LogManagement.info("Init get filtered orders ", this);
        ProcessOrderFilter processOrderFilter = getFilters(filter);
        var vector = functions.getFilteredProcessOrders(processOrderFilter);
        int count = functions.getFilteredProcessOrderCount(processOrderFilter);
        LogManagement.info("Finish get filtered orders ", this);

        List<ProcessOrder> processOrders;
        LogManagement.info("Init map filtered orders ", this);
        List<CompletableFuture<ProcessOrder>> futures = new ArrayList<>();
        Semaphore semaphore = new Semaphore(semaphorePool, true);

        for (Object item : vector) {
            if (item instanceof com.datasweep.compatibility.client.ProcessOrder po) {

                futures.add(CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                semaphore.acquire();
                                return buildProcessOrderPaged(
                                        po,
                                        processOrderFilter.getServer(),
                                        filter.isAll());
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
        LogManagement.info("Finish map filtered orders ", this);

        processOrders = futures.stream().map(CompletableFuture::join)
                .filter(Objects::nonNull).toList();
        int pageNumber = filter.getNumPage() != null ? filter.getNumPage() : 1;
        return new PageResponse<>(processOrders, pageNumber, processOrders.size(), count);
    }


    private ProcessOrder buildProcessOrder(com.datasweep.compatibility.client.ProcessOrder po, ServerImpl server, boolean allFilter) {
        LocalDateTime timeServer = LocalDateTime.now();
        if (po.getProcessOrderItems().get(0) != null) {
            ProcessOrderItem processOrderItem = (ProcessOrderItem) po.getProcessOrderItems().get(0);
            if(!OrderUtils.isWorkflowOrder(processOrderItem)){
                ProcessOrder poi = mapper.toProcessOrder(processOrderItem,server,orderStepOutputService,orderAppendService,executionService, allFilter);
                poi.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));
                return poi;
            }
        }
        return null;
    }

    private ProcessOrder buildProcessOrderPaged(com.datasweep.compatibility.client.ProcessOrder po, ServerImpl server, boolean allFilter) {
        if (po.getProcessOrderItems().get(0) != null) {
            ProcessOrderItem processOrderItem = (ProcessOrderItem) po.getProcessOrderItems().get(0);
            ProcessOrder poi = mapper.toProcessOrder(processOrderItem,server,orderStepOutputService,orderAppendService,executionService, allFilter);
            poi.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));
            return poi;
        }
        return null;
    }

    private ProcessOrderFilter getFilters(OrderFilter filter) {
        try {
            ProcessOrderFilter processOrderFilter = functions.createProcessOrderFilter();
            Calendar calendar = Calendar.getInstance();
            if (filter.getInitCreationDate() != null && filter.getFinishCreationDate() != null) {
                processOrderFilter.forCreationTimeGreaterThanOrEqualTo(DateFormatUtil.convertToSqlTime(filter.getInitCreationDate()));
                processOrderFilter.forCreationTimeLessThan(DateFormatUtil.convertToSqlTime(filter.getFinishCreationDate()));
                LogManagement.info(String.format(ADD_FILTER_CREATION_TIME_GREATER_THAN_OR_EQUAL_TO_S_FOR_CREATION_TIME_LESS_THAN, filter.getInitCreationDate(), filter.getFinishCreationDate()),this);
            }
            //ITEM FILTER
            ProcessOrderItemFilter itemFilter=functions.createProcessOrderItemFilter();
            if(filter.getStatus()!=null) {
                for (String state : filter.getStatus()) {
                    itemFilter.forCurrentStateContaining(state);
                }
            }
            setAccessPrivilegeOrReturnEmptyData(filter.getAccessPrivilege(), itemFilter, functions);

            if(filter.getName()!=null){
                processOrderFilter.forNameContaining(filter.getName());
            }

            itemFilter.forUdaEqualTo(UdaConstant.PROCESSING_TYPE, S88ProcessingType.BATCH.getChoiceElement().getValue());
            if(filter.getErpStartDate()!=null && filter.getErpFinishDate()!=null) {
                LogManagement.info("Init create subfilter ",this);
                itemFilter.forUdaGreaterThanOrEqualTo(UdaConstant.ERP_FINISH_DATE,toDsTimeSystem(filter.getErpStartDate()));
                itemFilter.forUdaLessThan(UdaConstant.ERP_START_DATE, toDsTimeSystem(filter.getErpFinishDate()));

            }else{
                if(filter.getInitCreationDate() == null && filter.getFinishCreationDate() == null) {
                    if (filter.getLastMonthsInterval() != null) {
                        LogManagement.info("apply cache filter :" + filter.getLastMonthsInterval(), this);
                        processOrderFilter.forCreationTimeGreaterThanOrEqualTo(getTimeTruncate(calendar, filter.getLastMonthsInterval()));
                    } else {

                        LogManagement.info("apply default filter month ``filters.default.monthInterval`` :" + monthInterval, this);
                        processOrderFilter.forCreationTimeGreaterThanOrEqualTo(getTimeTruncate(calendar, monthInterval));
                    }
                }
            }
            LogManagement.info("Init create addSearchForSubFilter ",this);
            processOrderFilter.addSearchForSubFilter(IKeyedFilterAttributes.KEY //campo que se usa para enlazar
                    , IFilterComparisonOperators.IN // operador de igualdad para subfiltros
                    , IProcessOrderItemFilterAttributes.ORDER_KEY // campo que se hace referencia del padre
                    , itemFilter);
            LogManagement.info("Finish create addSearchForSubFilter ",this);

            LogManagement.info(processOrderFilter.toString(),this);
            return processOrderFilter;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            LogManagement.error(String.format(ERROR_WHILE_PARSING_CREATION_DATES_INIT_CREATION_DATE_AND_FINISH_CREATION_DATE, filter.getInitCreationDate(), filter.getFinishCreationDate(), e),this);
            throw new RuntimeException(ERROR_WHILE_PARSING_CREATION_DATES, e);

        }
    }


    private ProcessOrderItemFilter getFilters(OrderDetailFilter filter) {
        ProcessOrderItemFilter processOrderItemFilter = functions.createProcessOrderItemFilter();
        if(filter.getName()!=null && !filter.getName().isEmpty()) {
            processOrderItemFilter.addSearchBy((short) 3, (short) 1, filter.getName());
        }
        if(filter.getKey()!=null) {
            processOrderItemFilter.addSearchBy((short) 2, (short) 1, filter.getKey());
        }
        LogManagement.info(processOrderItemFilter.toString(),this);
        return processOrderItemFilter;
    }

    private ProcessOrderFilter getFilters(OrderFilterPaged filter) {
        try {
            ProcessOrderFilter processOrderFilter = functions.createProcessOrderFilter();
            Calendar calendar = Calendar.getInstance();
            LogManagement.info("Prefilter ",this);
            if (filter.getInitCreationDate() != null && filter.getFinishCreationDate() != null) {
                LogManagement.info("Init creation date filter ",this);
                processOrderFilter.forCreationTimeGreaterThanOrEqualTo(DateFormatUtil.convertToSqlTime(filter.getInitCreationDate()));
                processOrderFilter.forCreationTimeLessThan(DateFormatUtil.convertToSqlTime(filter.getFinishCreationDate()));
                LogManagement.info(String.format(ADD_FILTER_CREATION_TIME_GREATER_THAN_OR_EQUAL_TO_S_FOR_CREATION_TIME_LESS_THAN, filter.getInitCreationDate(), filter.getFinishCreationDate()),this);

            }

            if(filter.getNumRows()!=null && filter.getNumPage()!=null) {
                LogManagement.info("Init get page filter ",this);
                processOrderFilter.setPagingFilterRowCount(filter.getNumRows());
                Integer numPage=(filter.getNumPage()>0)?filter.getNumPage()-1:0;
                processOrderFilter.setPagingFilterStartRow(numPage*filter.getNumRows());
                LogManagement.info("Start row: "+numPage*filter.getNumRows(),this);
                LogManagement.info("Finish get page filter ",this);
            }

            LogManagement.info("Init create subfilter ",this);
            //ITEM FILTER
            ProcessOrderItemFilter itemFilter=functions.createProcessOrderItemFilter();

            itemFilter.forUdaEqualTo(UdaConstant.PROCESSING_TYPE, S88ProcessingType.BATCH.getChoiceElement().getValue());
            setAccessPrivilegeOrReturnEmptyData(filter.getAccessPrivilege(), itemFilter, functions);
            if(filter.getStatus()!=null) {
                for (String state : filter.getStatus()) {
                    itemFilter.forCurrentStateContaining(state);
                }
            }

            if(filter.getErpStartDate()!=null && filter.getErpFinishDate()!=null) {
                itemFilter.forUdaGreaterThanOrEqualTo(UdaConstant.ERP_FINISH_DATE,toDsTimeSystem(filter.getErpStartDate()));
                itemFilter.forUdaLessThan(UdaConstant.ERP_START_DATE, toDsTimeSystem(filter.getErpFinishDate()));
            }else{
                if (filter.getInitCreationDate() == null && filter.getFinishCreationDate() == null) {
                    if (filter.getLastMonthsInterval() != null) {
                        LogManagement.info("apply cache filter :" + filter.getLastMonthsInterval(), this);
                        processOrderFilter.forCreationTimeGreaterThanOrEqualTo(getTimeTruncate(calendar, filter.getLastMonthsInterval()));
                    } else {
                        LogManagement.info("apply default filter month ``filters.default.monthInterval`` :" + monthInterval, this);
                        processOrderFilter.forCreationTimeGreaterThanOrEqualTo(getTimeTruncate(calendar, monthInterval));
                    }
                }
            }
            LogManagement.info("Init create addSearchForSubFilter ",this);
            processOrderFilter.addSearchForSubFilter(  IKeyedFilterAttributes.KEY //campo que se usa para enlazar
                    , IFilterComparisonOperators.IN // operador de igualdad para subfiltros
                    , IProcessOrderItemFilterAttributes.ORDER_KEY // campo que se hace referencia del padre
                    , itemFilter);
            LogManagement.info("Finish create addSearchForSubFilter ",this);
            //LogManagement.info(processOrderFilter.toString(),this);
            return processOrderFilter;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            LogManagement.error(String.format(ERROR_WHILE_PARSING_CREATION_DATES_INIT_CREATION_DATE_AND_FINISH_CREATION_DATE, filter.getInitCreationDate(), filter.getFinishCreationDate(), e),this);
            throw new RuntimeException(ERROR_WHILE_PARSING_CREATION_DATES, e);

        }
    }


    private Time getTimeTruncate(Calendar calendar, int monthInterval){
        calendar.add(Calendar.MONTH, -monthInterval);
        calendar= DateFormatUtil.truncateToDay(calendar);
        return new Time(calendar);
    }
    private static Time toDsTimeSystem(String iso8601Z) {
        Instant instant = Instant.parse(iso8601Z);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(instant.toEpochMilli());
        return new Time(cal).toUniversalTime();
    }

}
