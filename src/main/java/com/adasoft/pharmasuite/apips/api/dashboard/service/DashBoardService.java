package com.adasoft.pharmasuite.apips.api.dashboard.service;

import com.adasoft.pharmasuite.apips.api.dashboard.domain.Dashboard;
import com.adasoft.pharmasuite.apips.api.dashboard.domain.DashboardFilter;

import org.springframework.http.ResponseEntity;

public interface DashBoardService {
    ResponseEntity<Dashboard> getAllExceptionsSummary(DashboardFilter filter);
}
