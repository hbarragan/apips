package com.adasoft.pharmasuite.apips.api.order.config;

import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.api.order.service.ProcessOrdersService;
import com.adasoft.pharmasuite.apips.api.order.service.impl.ProcessOrdersServiceImpl;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.commons.base.ifc.services.PCContext;
import com.rockwell.mes.commons.base.ifc.services.ServiceFactory;
import com.rockwell.mes.services.commons.ifc.order.IOrderStepOutputService;
import com.rockwell.mes.services.order.ifc.IMESOrderService;
import com.rockwell.mes.services.order.impl.MESOrderService;
import com.rockwell.mes.services.s88.ifc.IS88ExecutionService;
import com.rockwell.mes.services.s88.ifc.IS88OrderAppendService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;

@Configuration
@DependsOn("apiConfig")
public class OrderConfig {


    @Bean
    public IMESOrderService getMESOrdersService() {
        return new MESOrderService();
    }

    @Bean
    public IOrderStepOutputService orderStepOutputService() {
        return ServiceFactory.getService(IOrderStepOutputService.class);
    }
    @Bean
    public IS88OrderAppendService orderAppendService(){
        return ServiceFactory.getService(IS88OrderAppendService .class);
    }
    @Bean
    public IS88ExecutionService executionService(){
        return ServiceFactory.getService(IS88ExecutionService.class);
    }



    @Bean
    public ProcessOrdersService processOrdersService(IFunctionsEx functions, IOrderStepOutputService orderStepOutputService, IS88OrderAppendService orderAppendService, IS88ExecutionService executionService,
                                                     Gson gsonMapper, ObjectMapper objectMapper,
                                                     MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService,
                                                     WebSocketHandler webSocketHandler,
                                                     TaskExecutor executor,
                                                     ApplicationEdmProvider applicationEdmProvider) {
        return new ProcessOrdersServiceImpl(functions,orderStepOutputService,orderAppendService,executionService, gsonMapper, objectMapper, memoryCacheService,
                jobQuartzService, webSocketHandler, executor, applicationEdmProvider);
    }


}