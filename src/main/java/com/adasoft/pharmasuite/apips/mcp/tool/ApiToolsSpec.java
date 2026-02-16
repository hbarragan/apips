package com.adasoft.pharmasuite.apips.mcp.tool;

import com.adasoft.pharmasuite.apips.health.controller.HealthController;
import com.adasoft.pharmasuite.apips.health.domain.HealthWebsocketResponse;
import com.adasoft.pharmasuite.apips.health.domain.SubscriptionsData;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;


import org.springframework.beans.factory.annotation.Value;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.Set;

@Component
public class ApiToolsSpec {

    @Value("${mcp.server.name:unknown}")
    private String mcpServerName;

    @Value("${server.application.version:unknown}")
    private String appVersion;

    @Value("${app.security.sso.enabled:false}")
    private boolean ssoEnabled;

    @Value("${app.security.https.enabled:false}")
    private boolean httpsEnabled;

    @Value("${app.security.https.http.port:0}")
    private int httpPort;

    @Value("${server.port:0}")
    private int serverPort;

    @Value("${app.custom.buildtime.plantOperationsUrl:}")
    private String plantOperationsUrl;

    @Value("${filters.default.monthInterval:0}")
    private int defaultMonthInterval;

    private final JobQuartzService jobQuartzService;

    public ApiToolsSpec(JobQuartzService jobQuartzService) {
        this.jobQuartzService = jobQuartzService;
    }


    @Tool(
            name = "PS API - System: ping",
            description = "Return pong to verify server availability and connectivity."
    )
    public String ping() {
        return "pong @ " + Instant.now();
    }

    @Tool(
            name = "PS API - System: info",
            description = "Return server version and runtime configuration information."
    )
    public String serverInfo() {
        return
                "MCP Server\n" +
                        "- name: " + mcpServerName + "\n" +
                        "- version: " + appVersion + "\n\n" +

                        "Security\n" +
                        "- sso.enabled: " + ssoEnabled + "\n" +
                        "- https.enabled: " + httpsEnabled + "\n" +
                        "- http.port: " + (httpPort == 0 ? "disabled" : httpPort) + "\n\n" +

                        "Server\n" +
                        "- server.port: " + serverPort + "\n\n" +

                        "URLs\n" +
                        "- plantOperationsUrl: " + plantOperationsUrl + "\n\n" +

                        "Filters\n" +
                        "- default.monthInterval: " + defaultMonthInterval;
    }

    @Tool(
            name = "PS API - System: status",
            description = "Return JVM memory usage, JVM thread metrics and HTTP thread pool status."
    )
    public String runtimeStatus() {

        // ===== JVM =====
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapCommitted = memory.getHeapMemoryUsage().getCommitted();
        long heapMax = memory.getHeapMemoryUsage().getMax();

        int totalThreads = threads.getThreadCount();
        int daemonThreads = threads.getDaemonThreadCount();
        int peakThreads = threads.getPeakThreadCount();

        // ===== TOMCAT (JMX) =====
        Integer maxThreads = null;
        Integer busyThreads = null;
        Integer currentThreads = null;
        Integer idleThreads = null;
        String poolName = null;

        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> pools = mbs.queryNames(
                    new ObjectName("Tomcat:type=ThreadPool,*"), null
            );

            if (pools == null || pools.isEmpty()) {
                pools = mbs.queryNames(
                        new ObjectName("Catalina:type=ThreadPool,*"), null
                );
            }

            ObjectName pool = chooseHttpPool(pools);
            if (pool != null) {
                poolName = pool.getKeyProperty("name");
                maxThreads = getIntAttr(mbs, pool, "maxThreads");
                busyThreads = getIntAttr(mbs, pool, "currentThreadsBusy");
                currentThreads = getIntAttr(mbs, pool, "currentThreadCount");
                if (currentThreads != null && busyThreads != null) {
                    idleThreads = Math.max(0, currentThreads - busyThreads);
                }
            }
        } catch (Exception ignored) {
        }

        return
                "JVM Memory\n" +
                        "- heap.used: " + mb(heapUsed) + "\n" +
                        "- heap.committed: " + mb(heapCommitted) + "\n" +
                        "- heap.max: " + mb(heapMax) + "\n\n" +

                        "JVM Threads\n" +
                        "- threads.total: " + totalThreads + "\n" +
                        "- threads.daemon: " + daemonThreads + "\n" +
                        "- threads.peak: " + peakThreads + "\n\n" +

                        "HTTP Thread Pool (Tomcat)\n" +
                        "- pool: " + (poolName == null ? "n/a" : poolName) + "\n" +
                        "- threads.max: " + val(maxThreads) + "\n" +
                        "- threads.current: " + val(currentThreads) + "\n" +
                        "- threads.busy: " + val(busyThreads) + "\n" +
                        "- threads.idle: " + val(idleThreads) + "\n\n" +

                        "Runtime\n" +
                        "- uptime: " + (runtime.getUptime() / 1000) + " s";
    }

    @Tool(
            name = "PS API - System: health session quartz",
            description = "Provides system health information including active sessions and Quartz scheduled jobs."
    )
    public HealthWebsocketResponse getInfoSessionQuartz(){
        HealthWebsocketResponse response = new HealthWebsocketResponse();
        response.setSubscriptionsData(HealthController.buildSubsBySession());
        response.setJobs(jobQuartzService.getAllJobDetails());
        return response;
    }

    private static ObjectName chooseHttpPool(Set<ObjectName> pools) {
        if (pools == null || pools.isEmpty()) return null;
        for (ObjectName on : pools) {
            String name = on.getKeyProperty("name");
            if (name != null && name.toLowerCase().contains("http")) {
                return on;
            }
        }
        return pools.iterator().next();
    }

    private static Integer getIntAttr(MBeanServer mbs, ObjectName on, String attr) {
        try {
            Object v = mbs.getAttribute(on, attr);
            return (v instanceof Number n) ? n.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String mb(long bytes) {
        return bytes < 0 ? "unlimited" : (bytes / (1024 * 1024)) + " MB";
    }

    private static String val(Integer v) {
        return v == null ? "n/a" : v.toString();
    }
}
