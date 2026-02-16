package com.adasoft.pharmasuite.apips.core.config;

import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.cache.service.impl.MemoryCacheServiceImpl;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzServiceImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.commons.base.ifc.services.PCContext;
import jakarta.annotation.PostConstruct;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiConfig {
    @Value("${websocket.time}")
    private long defaultTimeout;

    @Value("${app.custom.buildtime.url}")
    private String url;

    @Value("${app.custom.enable}")
    private boolean enabledCustom;

    @Value("${app.custom.buildtime.remoteUrl}")
    private String remoteUrl;

    @Value("${app.custom.buildtime.user}")
    private String userProps;

    @Value("${app.custom.buildtime.pass}")
    private String passProps;

    @Value("${app.custom.async.core-pool-size}")
    private int corePoolSize;

    @Value("${app.custom.async.max-pool-size}")
    private int maxPoolSize;

    @Value("${app.custom.async.queue-capacity}")
    private int queueCapacity;

    @Value("${app.custom.async.thread-name-prefix}")
    private String threadNamePrefix;

    @Value("${app.custom.async.keep-alive-seconds}")
    private int keepAliveSeconds;

    private final Environment environment;

    public ApiConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void initProperties() {

        if (enabledCustom) {
            String remoteUrlLocal = environment.getProperty("app.custom.buildtime.remoteUrl", remoteUrl);
            String baseUrl = environment.getProperty("app.custom.buildtime.baseUrl", remoteUrlLocal);
            String plantUrl = environment.getProperty("app.custom.buildtime.plantOperationsUrl", remoteUrlLocal);
            String logLevel = environment.getProperty("app.custom.buildtime.logLevel", "INFO");
            String user = environment.getProperty("app.custom.auth.user", userProps);
            String pass = environment.getProperty("app.custom.auth.password", passProps);
            String[] pcArgs = new String[] { "remote+" + remoteUrl, baseUrl, plantUrl, baseUrl, plantUrl, baseUrl,
                    logLevel, user, pass };

            System.setProperty("buildtime.remoteUrl", remoteUrl);
            System.setProperty("buildtime.baseUrl", baseUrl);
            System.setProperty("buildtime.plantOperationsUrl", plantUrl);
            System.setProperty("buildtime.logLevel", logLevel);
            System.setProperty("com.rockwell.test.username", user);
            System.setProperty("com.rockwell.test.password", pass);

            //check config to initialize PCContext with custom args
            PCContext.setProgramArguments(pcArgs);
            return;
        }

        System.setProperty("user.language", environment.getProperty("app.custom.jvm-params.language", "en"));
        System.setProperty("user.region", environment.getProperty("app.custom.jvm-params.region", "us"));
        System.setProperty("javax.net.ssl.trustStoreType",
                environment.getProperty("app.custom.jvm-params.sslTrustStoreType", "Windows-ROOT"));
        System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES",
                environment.getProperty("app.custom.jvm-params.activemqSerializablePackages", "*"));
        System.setProperty("uiDefaultButtonFollowFocus",
                environment.getProperty("app.custom.jvm-params.defaultButtonFollowFocus", "false"));
        System.setProperty("buildtime.remoteUrl",
                environment.getProperty("app.custom.buildtime.remoteUrl", "http://localhost"));
        System.setProperty("buildtime.baseUrl",
                environment.getProperty("app.custom.buildtime.baseUrl", "http://localhost"));
        System.setProperty("buildtime.plantOperationsUrl",
                environment.getProperty("app.custom.buildtime.plantOperationsUrl", "http://localhost"));
        System.setProperty("buildtime.logLevel", environment.getProperty("app.custom.buildtime.logLevel", "INFO"));

    }

    @Bean
    public Gson gsonMapper() {
        return new GsonBuilder()
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper.configOverride(LocalDateTime.class)
                .setInclude(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS));

        return objectMapper;
    }

    @Bean
    public JobQuartzService getQuartzService(Scheduler scheduler) {
        return new JobQuartzServiceImpl(scheduler, defaultTimeout);
    }

    @Bean
    WebSocketHandler getWebSocketHandler(JobQuartzService jobQuartzService) {
        return new WebSocketHandler(jobQuartzService);
    }

    @Lazy
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        return new SchedulerFactoryBean();
    }

    @Lazy
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean factory) throws SchedulerException {
        Scheduler scheduler = factory.getScheduler();
        scheduler.start();
        return scheduler;
    }

    @Lazy
    @Bean
    public IFunctionsEx functions() {
        return PCContext.getFunctions();
    }

    @Bean
    public MemoryCacheService getMemoryCacheService() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return new MemoryCacheServiceImpl(md);
    }

    @Bean
    public TaskExecutor executor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }


}