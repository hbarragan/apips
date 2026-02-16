package com.adasoft.pharmasuite.apips.api.workflow.service;

import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.workflow.domain.Workflow;
import com.adasoft.pharmasuite.apips.api.workflow.domain.WorkflowFilter;

import org.springframework.http.ResponseEntity;

import java.util.List;

public interface WorkflowService {
    ResponseEntity<PageResponseOdata<Workflow>> getFilteredOData(final OdataPage odataPage);
}
