package com.adasoft.pharmasuite.apips.websocket.service;


import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.domain.SchedulerEntry;
import com.adasoft.pharmasuite.apips.websocket.job.GenericQuartzJob;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobQuartzServiceImpl implements JobQuartzService {

    private final Map<String, SchedulerEntry> schedulers = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final Long defaultTime;

    public JobQuartzServiceImpl(Scheduler scheduler, Long defaultTime) {
        this.scheduler = scheduler;
        this.defaultTime= defaultTime;
    }

    public void create(String varName, SchedulerEntry entry) {
        if (schedulers.containsKey(varName)) return;

        try {
            schedulers.put(varName, entry);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("uri", entry.getUri());
            jobDataMap.put("queryString", entry.getQueryString());
            jobDataMap.put("varName", varName);

            JobDetail job = JobBuilder.newJob(GenericQuartzJob.class)
                    .withIdentity(varName, "websocket-jobs")
                    .usingJobData(jobDataMap)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(varName + "Trigger", "websocket-triggers")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(entry.getTime()!=null?entry.getTime():defaultTime)
                            .repeatForever())
                    .build();
            scheduler.scheduleJob(job, trigger);
        } catch (Exception e) {
            schedulers.remove(varName);
            throw new RuntimeException("Error creating scheduler for " + varName, e);
        }
    }

    public boolean exist(String varName) {
        return schedulers.containsKey(varName);
    }

    public void remove(String websocketName) {
        schedulers.remove(websocketName);
        try {
            JobKey jobKey = new JobKey(websocketName, "websocket-jobs");
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                LogManagement.info("Delete job :" + websocketName, this.getClass());
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Error removing scheduled job for " + websocketName, e);
        }
    }
    @Override
    public List<String> getAllJobKeys() {
        try {
            List<String> jobKeysList = new ArrayList<>();
            for (String groupName : scheduler.getJobGroupNames()) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
                for (JobKey jobKey : jobKeys) {
                    jobKeysList.add(jobKey.getName());
                }
            }
            return jobKeysList;
        } catch (SchedulerException e) {
            throw new RuntimeException("Error getting job keys", e);
        }
    }

    @Override
    public List<Map<String, Object>> getAllJobDetails() {
        try {
            List<Map<String, Object>> detailsList = new ArrayList<>();
            for (String groupName : scheduler.getJobGroupNames()) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
                for (JobKey jobKey : jobKeys) {
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                    Map<String, Object> detailMap = new HashMap<>();
                    detailMap.put("name", jobKey.getName());
                    detailMap.put("group", jobKey.getGroup());
                    detailMap.put("description", jobDetail.getDescription());
                    detailMap.put("jobDataMap", jobDetail.getJobDataMap());
                    detailsList.add(detailMap);
                }
            }
            return detailsList;
        } catch (SchedulerException e) {
            throw new RuntimeException("Error getting job details", e);
        }
    }

    @Override
    public List<Map<String, Object>> getAllJobs() {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (String groupName : scheduler.getJobGroupNames()) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
                for (JobKey jobKey : jobKeys) {
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    for (Trigger trigger : triggers) {
                        Map<String, Object> jobData = new HashMap<>();
                        jobData.put("jobName", jobKey.getName());
                        jobData.put("group", jobKey.getGroup());
                        jobData.put("triggerType", trigger.getClass().getSimpleName());
                        jobData.put("nextFireTime", trigger.getNextFireTime());
                        jobData.put("previousFireTime", trigger.getPreviousFireTime());
                        result.add(jobData);
                    }
                }
            }
            return result;
        } catch (SchedulerException e) {
            throw new RuntimeException("Error getting all jobs", e);
        }
    }

    @Override
    public boolean triggerJob(String jobName) {
        try {
            JobKey jobKey = new JobKey(jobName, "websocket-jobs");
            if (scheduler.checkExists(jobKey)) {
                scheduler.triggerJob(jobKey);
                return true;
            }
            return false;
        } catch (SchedulerException e) {
            throw new RuntimeException("Error triggering job " + jobName, e);
        }
    }

    @Override
    public boolean deleteJob(String jobName) {
        try {
            JobKey jobKey = new JobKey(jobName, "websocket-jobs");
            return scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException("Error deleting job " + jobName, e);
        }
    }

    @Override
    public boolean deleteAllJobs() {
        try {
            boolean result = true;
            for (String groupName : scheduler.getJobGroupNames()) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
                result &= scheduler.deleteJobs(new ArrayList<>(jobKeys));
            }
            return result;
        } catch (SchedulerException e) {
            throw new RuntimeException("Error deleting all jobs", e);
        }
    }


}


