package com.adasoft.pharmasuite.apips.api.workflow.controller;

import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.workflow.domain.Workflow;
import com.adasoft.pharmasuite.apips.api.workflow.service.WorkflowService;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.adasoft.pharmasuite.apips.api.common.controller.CommonController.getOdataPage;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value=ApiConstants.API_WORKFLOW, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = ApiConstants.TAG_WORKFLOW, description = ApiConstants.DESCRIPTION_WORKFLOW)
public class WorkflowController {
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }


    @GetMapping(ApiConstants.PATH_PAGED)
    @Operation(
            summary = "Get filtered workflow process order paged",
            description = "Returns a list of workflow filtered and paged.",
            operationId = "getWorkflowProcessOrdersPaged",
            tags = { ApiConstants.TAG_WORKFLOW }
    )
    public ResponseEntity<PageResponseOdata<Workflow>> getFilteredBatchPaged(HttpServletRequest request,
                                                                            @RequestParam(name = "$filter", required = false) String filter,
                                                                            @RequestParam(name = "$orderby", required = false) String orderBy,
                                                                            @RequestParam(name = "$top", required = false) Integer top,
                                                                            @RequestParam(name = "$skip", required = false) Integer skip,
                                                                            @RequestParam(name = "$count", required = false) Boolean count) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);
        return workflowService.getFilteredOData(odataPage);
    }

}
