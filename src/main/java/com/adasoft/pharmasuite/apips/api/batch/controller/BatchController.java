package com.adasoft.pharmasuite.apips.api.batch.controller;

import com.adasoft.pharmasuite.apips.api.batch.domain.Batch;
import com.adasoft.pharmasuite.apips.api.batch.domain.Sublot;
import com.adasoft.pharmasuite.apips.api.batch.domain.SublotFilter;
import com.adasoft.pharmasuite.apips.api.batch.domain.TransactionHistory;
import com.adasoft.pharmasuite.apips.api.batch.domain.TransactionHistoryFilter;
import com.adasoft.pharmasuite.apips.api.batch.service.BatchService;
import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.adasoft.pharmasuite.apips.api.common.controller.CommonController.getOdataPage;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = ApiConstants.API_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = ApiConstants.TAG_BATCH, description = ApiConstants.DESCRIPTION_BATCH)
public class BatchController  {

    public static final String DESCRIPTION_BATCH_PAGED = "Returns a page of batch filtered.";
    public static final String OPERATION_ID_BATCH_PAGED = "getFilteredBatchPaged";
    public static final String OPERATION_ID_SUBLOT_GET_ALL = "getAllSublot";
    public static final String DESCRIPTION_SUBLOT_GET_ALL = "Returns a list of sublot filtered.";
    public static final String SUMMARY_GET_SUBLOT_GET_ALL = "Get filtered sublot";

    public static final String OPERATION_ID_TRANSACTION_HISTORY_PAGED = "getTransactionHistoryPaged";
    public static final String DESCRIPTION_TRANSACTION_HISTORY_PAGED = "Returns a list of transaction history filtered paged.";

    public static final String OPERATION_ID_TRANSACTION_HISTORY_GET_ALL = "getAllTransactionHistory";
    public static final String DESCRIPTION_TRANSACTION_HISTORY_GET_ALL = "Returns a list of transaction history filtered.";
    public static final String SUMMARY_GET_TRANSACTION_HISTORY_GET_ALL = "Get filtered transaction history";

    private final BatchService batchService;

    public BatchController(BatchService service) {
        this.batchService = service;
    }


    @GetMapping(ApiConstants.API_SUBLOT)
    @Operation(
            summary = SUMMARY_GET_SUBLOT_GET_ALL,
            description = DESCRIPTION_SUBLOT_GET_ALL,
            operationId = OPERATION_ID_SUBLOT_GET_ALL,
            tags = { ApiConstants.TAG_BATCH}
    )
    public ResponseEntity<List<Sublot>> getFilteredSubLot(@ParameterObject SublotFilter filter, HttpServletRequest request) {
        return batchService.getFilteredSubLot(filter);
    }

    @GetMapping(ApiConstants.API_TRANSACTION_HISTORY+ApiConstants.PATH_PAGED)
    @Operation(
            summary = "Get filtered transaction history (OData v4)",
            description = DESCRIPTION_TRANSACTION_HISTORY_PAGED,
            operationId = OPERATION_ID_TRANSACTION_HISTORY_PAGED,
            tags = { ApiConstants.TAG_BATCH }
    )
    public ResponseEntity<PageResponseOdata<TransactionHistory>> getFilteredTransactionHistoryPaged(HttpServletRequest request,
                                                                                                  @RequestParam(name = "$filter", required = false) String filter,
                                                                                                  @RequestParam(name = "$orderby", required = false) String orderBy,
                                                                                                  @RequestParam(name = "$top", required = false) Integer top,
                                                                                                  @RequestParam(name = "$skip", required = false) Integer skip,
                                                                                                  @RequestParam(name = "$count", required = false) Boolean count) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);
        return batchService.getFilteredTransactionHistoryOData(odataPage);
    }

    @GetMapping(ApiConstants.API_TRANSACTION_HISTORY)
    @Operation(
            summary = SUMMARY_GET_TRANSACTION_HISTORY_GET_ALL,
            description = DESCRIPTION_TRANSACTION_HISTORY_GET_ALL,
            operationId = OPERATION_ID_TRANSACTION_HISTORY_GET_ALL,
            tags = { ApiConstants.TAG_BATCH}
    )
    public ResponseEntity<List<TransactionHistory>> getFilteredTransactionHistory(@ParameterObject TransactionHistoryFilter filter, HttpServletRequest request) {
        return batchService.getFilteredTransactionHistory(filter);
    }

    @GetMapping(ApiConstants.PATH_PAGED)
    @Operation(
            summary = "Get filtered batch (OData v4)",
            description = DESCRIPTION_BATCH_PAGED,
            operationId = OPERATION_ID_BATCH_PAGED,
            tags = { ApiConstants.TAG_BATCH }
    )
    public ResponseEntity<PageResponseOdata<Batch>> getFilteredBatchPaged(HttpServletRequest request,
                                                                          @RequestParam(name = "$filter", required = false) String filter,
                                                                          @RequestParam(name = "$orderby", required = false) String orderBy,
                                                                          @RequestParam(name = "$top", required = false) Integer top,
                                                                          @RequestParam(name = "$skip", required = false) Integer skip,
                                                                          @RequestParam(name = "$count", required = false) Boolean count) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);
        return batchService.getFilteredBatchOData(odataPage);
    }

}
