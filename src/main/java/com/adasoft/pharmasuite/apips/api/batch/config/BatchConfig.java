package com.adasoft.pharmasuite.apips.api.batch.config;

import com.adasoft.pharmasuite.apips.api.batch.service.impl.BatchServiceImpl;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.services.inventory.impl.SublotService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;

@Configuration
@DependsOn("apiConfig")
public class BatchConfig {

    @Bean
    public BatchServiceImpl batchService(IFunctionsEx functions, Gson gsonMapper , ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, TaskExecutor executor, ApplicationEdmProvider applicationEdmProvider) {
        return new BatchServiceImpl(functions,gsonMapper ,objectMapper , memoryCacheService, new SublotService(), jobQuartzService, webSocketHandler, executor, applicationEdmProvider);
    }

}
