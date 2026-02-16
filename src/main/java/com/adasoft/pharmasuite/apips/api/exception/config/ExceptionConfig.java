package com.adasoft.pharmasuite.apips.api.exception.config;

import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.api.exception.service.ExceptionService;
import com.adasoft.pharmasuite.apips.api.exception.service.impl.ExceptionServiceImpl;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.deviation.ifc.IExceptionRecordingService;
import com.rockwell.mes.commons.deviation.impl.ExceptionRecordingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;

@Configuration
@DependsOn("apiConfig")
public class ExceptionConfig {
    @Bean
    public IExceptionRecordingService exceptionServicePS() { return new ExceptionRecordingService();}
    @Bean
    public ExceptionService exceptionService(Gson gsonMapper , ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler, TaskExecutor executor) {
        return new ExceptionServiceImpl(gsonMapper ,objectMapper ,new ExceptionRecordingService(), memoryCacheService, jobQuartzService, webSocketHandler, executor);
    }
}
