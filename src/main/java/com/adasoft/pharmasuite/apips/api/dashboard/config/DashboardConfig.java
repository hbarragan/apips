package com.adasoft.pharmasuite.apips.api.dashboard.config;

import com.adasoft.pharmasuite.apips.api.dashboard.domain.DashboardStatesProperties;
import com.adasoft.pharmasuite.apips.api.dashboard.service.impl.DashBoardServiceImpl;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.PCContext;
import com.rockwell.mes.commons.deviation.impl.ExceptionRecordingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn("apiConfig")
public class DashboardConfig {

    @Bean

    public DashBoardServiceImpl dashBoardService(Gson gsonMapper, ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, DashboardStatesProperties statesProps) {
        return new DashBoardServiceImpl(PCContext.getFunctions(), gsonMapper, objectMapper, memoryCacheService, jobQuartzService, new ExceptionRecordingService(), statesProps, webSocketHandler);
    }
}
