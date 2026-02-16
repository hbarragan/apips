package com.adasoft.pharmasuite.apips.api.order.controller;

import com.adasoft.pharmasuite.apips.api.common.domain.PageResponse;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderDetailFilter;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilter;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilterPaged;
import com.adasoft.pharmasuite.apips.api.order.domain.ProcessOrder;
import com.adasoft.pharmasuite.apips.api.order.domain.ProcessOrderDetail;
import com.adasoft.pharmasuite.apips.api.order.service.ProcessOrdersService;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value=ApiConstants.API_ORDER, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = ApiConstants.TAG_ORDER, description = ApiConstants.DESCRIPTION_ORDER)
public class OrderController {
    public static final String SUMMARY_GET_PROCESS_ORDER = "Get filtered orders";
    public static final String SUMMARY_GET_PROCESS_ORDER_DETAIL = "Get order detail";
    public static final String SUMMARY_GET_PROCESS_ORDER_PAGED = "Get filtered orders paged";
    public static final String DESCRIPTION_GET_PROCESS_ORDERS = "Returns a list of orders filtered by state, date range, and interval.";
    public static final String DESCRIPTION_GET_PROCESS_ORDER_DETAIL = "Returns a detail fields of a order.";
    public static final String OPERATION_ID_GET_PROCESS_ORDERS = "getProcessOrders";
    public static final String OPERATION_ID_GET_PROCESS_ORDERS_PAGED = "getProcessOrdersPaged";
    public static final String OPERATION_ID_GET_PROCESS_ORDER_DETAIL = "getProcessOrderDetail";
    private final ProcessOrdersService processOrdersService;


    public OrderController(ProcessOrdersService processOrdersService) {
        this.processOrdersService = processOrdersService;
    }

    @GetMapping(value=ApiConstants.API_PROCESS_ORDER, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = SUMMARY_GET_PROCESS_ORDER,
            description = DESCRIPTION_GET_PROCESS_ORDERS,
            operationId = OPERATION_ID_GET_PROCESS_ORDERS,
            tags = { ApiConstants.TAG_ORDER}
    )
    @Deprecated
    public ResponseEntity<List<ProcessOrder>> getProcessOrders(
            @ParameterObject OrderFilter filter,
            HttpServletRequest request) {
        filter.setRequestInfo(request);
        return processOrdersService.getAllProcessOrders(filter);
    }

    @GetMapping(value=ApiConstants.API_PROCESS_ORDER_PAGED, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = SUMMARY_GET_PROCESS_ORDER_PAGED,
            description = DESCRIPTION_GET_PROCESS_ORDERS,
            operationId = OPERATION_ID_GET_PROCESS_ORDERS_PAGED,
            tags = { ApiConstants.TAG_ORDER}
    )
    public ResponseEntity<PageResponse<ProcessOrder>> getProcessOrdersPaged(@ParameterObject OrderFilterPaged filter, HttpServletRequest request) {
        filter.setRequestInfo(request);
        return processOrdersService.getAllProcessOrdersPaged(filter);
    }


    @GetMapping(value=ApiConstants.API_PROCESS_ORDER+ApiConstants.API_DETAILS, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = SUMMARY_GET_PROCESS_ORDER_DETAIL,
            description = DESCRIPTION_GET_PROCESS_ORDER_DETAIL,
            operationId = OPERATION_ID_GET_PROCESS_ORDER_DETAIL,
            tags = { ApiConstants.TAG_ORDER}
    )
    public ResponseEntity<ProcessOrderDetail> getProcessOrderDetail(@ParameterObject OrderDetailFilter filter) {
        return processOrdersService.getProcessOrderDetail(filter);
    }



}
