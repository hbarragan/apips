package com.adasoft.pharmasuite.apips.websocket.service;

import com.adasoft.pharmasuite.apips.websocket.domain.SchedulerEntry;

import java.util.List;
import java.util.Map;

public interface JobQuartzService {
    void create(String varName, SchedulerEntry entry);
    boolean exist(String varName);
    void remove(String websocketName);

    // SchedulerController
    List<String> getAllJobKeys();
    List<Map<String, Object>> getAllJobDetails();
    List<Map<String, Object>> getAllJobs();
    boolean triggerJob(String jobName);
    boolean deleteJob(String jobName);
    boolean deleteAllJobs();

}