package com.adasoft.pharmasuite.apips.api.workflow.config;

import com.adasoft.pharmasuite.apips.api.common.service.impl.CommonEdmProvider;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.api.workflow.service.WorkflowService;
import com.adasoft.pharmasuite.apips.api.workflow.service.impl.WorkflowServiceImpl;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;

@Configuration
@DependsOn("apiConfig")
public class WorkflowConfig {

    @Bean
    public WorkflowService workflowService(IFunctionsEx functions, Gson gsonMapper, ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, TaskExecutor executor,  ApplicationEdmProvider applicationEdmProvider) {
        return new WorkflowServiceImpl(functions, gsonMapper, objectMapper, memoryCacheService, jobQuartzService, webSocketHandler, executor, applicationEdmProvider);
    }
}
