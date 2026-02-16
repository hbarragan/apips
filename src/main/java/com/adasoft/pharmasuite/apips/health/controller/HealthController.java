package com.adasoft.pharmasuite.apips.health.controller;

import com.adasoft.pharmasuite.apips.health.domain.*;
import com.adasoft.pharmasuite.apips.health.type.HealthyEnum;
import com.adasoft.pharmasuite.apips.websocket.domain.SubscriptionRegistry;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.rockwell.mes.commons.base.ifc.services.PCContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static com.adasoft.pharmasuite.apips.core.constant.ApiConstants.*;

@RestController
@Tag(name = TAG_HEALTH, description = DESCRIPTION_HEALTH)
public class HealthController {

    public static final String SUMMARY_GET_HEALTH = "Get health status";
    public static final String DESCRIPTION_GET_HEALTH = "Return health status.";
    public static final String OPERATION_ID_GET_HEALTH = "getHealth";
    public static final String MS = "0ms";
    public static final String VERSION_PCCONTEXT_GET_FUNCTIONS = "testConnectionPCContextGetFunctions";
    public static final String PCCONTEXT_FUNCTIONS_HEALTH = "Version of Functions. If error to conect to PCContext, it will be unhealthy";


    private final JobQuartzService jobQuartzService;

    public HealthController(JobQuartzService jobQuartzService) {
        this.jobQuartzService = jobQuartzService;
    }

    @GetMapping(API_HEALTH) // mismo path que HC_URL (/health)
    @Operation(
            summary = SUMMARY_GET_HEALTH,
            description = DESCRIPTION_GET_HEALTH,
            operationId = OPERATION_ID_GET_HEALTH,
            tags = {TAG_HEALTH}
    )
    public ResponseEntity<HealthStatus> getHealth() {
        long startTimeMillis = System.currentTimeMillis();

        Map<String, Entry> entries = buildEntries();
        HealthyEnum globalStatus = getStatusByEntries(entries);


        HealthStatus response = new HealthStatus();
        response.setStatus(globalStatus);
        response.setEntries(entries);
        response.setTotalDuration((System.currentTimeMillis() - startTimeMillis) + "ms");

        return ResponseEntity.ok(response);
    }

    private static HealthyEnum getStatusByEntries(Map<String, Entry> entries) {
        Entry pcContextEntry = entries.get(VERSION_PCCONTEXT_GET_FUNCTIONS);
        HealthyEnum globalStatus = HealthyEnum.HEALTHY;
        if (pcContextEntry != null && HealthyEnum.UNHEALTHY.equals(pcContextEntry.getStatus())) {
            globalStatus = HealthyEnum.UNHEALTHY;
        }
        return globalStatus;
    }

    private Map<String, Entry> buildEntries() {
        // 1) Recolectar la misma info que hoy
        SubscriptionsData subscriptionsData = buildSubsBySession();
        Object jobsData = jobQuartzService.getAllJobDetails();

        // 2) Montar respuesta HealthStatus
        Map<String, Entry> entries = new LinkedHashMap<>();
        entries.put(
                "websocketSubscriptions",
                new Entry(
                        HealthyEnum.HEALTHY,
                        new SubscriptionsEntryData(
                                subscriptionsData.sessions,
                                subscriptionsData.subsByMsg,
                                subscriptionsData.subsBySession
                        ),
                        MS,
                        null,
                        "Websocket session/subscription status"
                )
        );
        entries.put(
                "jobs",
                new Entry(
                        HealthyEnum.HEALTHY,
                        new JobsEntryData(jobsData),
                        MS,
                        null,
                        "Quartz jobs status"
                )
        );

        try {
            long startTimeMillisVersion = System.currentTimeMillis();

            FutureTask<String> task = new FutureTask<>(() -> {
                String version = PCContext.getFunctions().getVersion();
                PCContext.getFunctions().getAllLists();
                return version;
            });

            Thread thread = new Thread(task, "pccontext-health-check");
            thread.setDaemon(true);
            thread.start();

            String version = task.get(30, TimeUnit.SECONDS); // timeout 30s
            entries.put(
                    VERSION_PCCONTEXT_GET_FUNCTIONS,
                    new Entry(
                            HealthyEnum.HEALTHY,
                            new DataEntry(version),
                            (System.currentTimeMillis() - startTimeMillisVersion) + "ms",
                            null,
                            PCCONTEXT_FUNCTIONS_HEALTH
                    ));
        } catch (Exception e) {
            entries.put(
                    VERSION_PCCONTEXT_GET_FUNCTIONS,
                    new Entry(
                            HealthyEnum.UNHEALTHY,
                            new DataEntry("Error connect to server"),
                            MS, null, PCCONTEXT_FUNCTIONS_HEALTH
                    ));
        }
        return entries;
    }


    public static SubscriptionsData buildSubsBySession() {
        List<String> sessionIds = SubscriptionRegistry.getSessions()
                .stream()
                .map(WebSocketSession::getId)
                .toList();

        Map<String, List<String>> subsByMsgMap = new LinkedHashMap<>();
        SubscriptionRegistry.getSubsByMsg().forEach((varName, sessions) -> {
            subsByMsgMap.put(varName, sessions.stream().map(WebSocketSession::getId).toList());
        });

        Map<String, List<String>> subsBySessionMap = new LinkedHashMap<>();
        SubscriptionRegistry.getSubsBySession().forEach((session, varNames) -> {
            subsBySessionMap.put(session.getId(), new ArrayList<>(varNames));
        });

        return new SubscriptionsData(sessionIds, subsByMsgMap, subsBySessionMap);
    }
}
