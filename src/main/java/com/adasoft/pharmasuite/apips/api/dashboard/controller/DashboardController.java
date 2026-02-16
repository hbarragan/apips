package com.adasoft.pharmasuite.apips.api.dashboard.controller;

import com.adasoft.pharmasuite.apips.api.dashboard.domain.Dashboard;
import com.adasoft.pharmasuite.apips.api.dashboard.domain.DashboardFilter;
import com.adasoft.pharmasuite.apips.api.dashboard.service.DashBoardService;
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
import org.springframework.web.bind.annotation.RestController;

//@CrossOrigin(origins = "*")
//@RestController
//@RequestMapping(value = ApiConstants.API_DASHBOARD, produces = MediaType.APPLICATION_JSON_VALUE)
//@Tag(name = ApiConstants.TAG_DASHBOARD, description = ApiConstants.DESCRIPTION_DASHBOARD)
public class DashboardController {

    public static final String SUMMARY_GET_EXCEPTIONS_SUMMARY = "Get exceptions summary dashboard";
    public static final String DESCRIPTION_GET_EXCEPTIONS_SUMMARY = "Returns a list of exceptions summary dashboard.";
    public static final String OPERATION_ID_GET_EXCEPTIONS_SUMMARY = "getAllExceptionsSummary";

    private final DashBoardService dashBoardService;
    public DashboardController(DashBoardService dashBoardService) {
        this.dashBoardService = dashBoardService;
    }

    //@GetMapping(value = ApiConstants.API_DASHBOARD_EXCPETIONS_SUMMARY,produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = SUMMARY_GET_EXCEPTIONS_SUMMARY,
            description = DESCRIPTION_GET_EXCEPTIONS_SUMMARY,
            operationId = OPERATION_ID_GET_EXCEPTIONS_SUMMARY,
            tags = { ApiConstants.TAG_DASHBOARD}
    )
    public ResponseEntity<Dashboard> getAllExceptionsSummary(@ParameterObject DashboardFilter filter, HttpServletRequest request) {
        filter.setRequestInfo(request);
        return dashBoardService.getAllExceptionsSummary(filter);
    }


}
