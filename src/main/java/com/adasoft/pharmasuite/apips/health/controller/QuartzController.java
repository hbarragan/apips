package com.adasoft.pharmasuite.apips.health.controller;

import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.adasoft.pharmasuite.apips.core.constant.ApiConstants.*;

@RestController
@RequestMapping(API_SCHEDULER)
@Tag(name = TAG_SCHEDULER, description = DESCRIPTION_SCHEDULER)
public class QuartzController {


    public static final String SUMMARY_GET_JOB_KEYS = "Get all job keys";
    public static final String DESCRIPTION_GET_JOB_KEYS = "Return a list of job keys.";
    public static final String OPERATION_ID_GET_JOB_KEYS = "getJobKeys";

    public static final String SUMMARY_GET_JOB_DETAIL = "Get all job details";
    public static final String DESCRIPTION_GET_JOB_DETAIL = "Return a list of job details.";
    public static final String OPERATION_ID_GET_JOB_DETAIL = "getJobDetails";

    public static final String SUMMARY_GET_ALL_JOBS = "Get all jobs";
    public static final String DESCRIPTION_GET_ALL_JOBS = "Return a list of all jobs with full info.";
    public static final String OPERATION_ID_GET_ALL_JOBS = "getAllJobs";

    public static final String SUMMARY_TRIGGER_JOB = "Trigger a job";
    public static final String DESCRIPTION_TRIGGER_JOB = "Trigger a specific job immediately by name.";
    public static final String OPERATION_ID_TRIGGER_JOB = "triggerJob";

    public static final String SUMMARY_DELETE_JOB = "Delete a job";
    public static final String DESCRIPTION_DELETE_JOB = "Delete a specific job by name.";
    public static final String OPERATION_ID_DELETE_JOB = "deleteJob";

    public static final String SUMMARY_DELETE_ALL_JOBS = "Delete all jobs";
    public static final String DESCRIPTION_DELETE_ALL_JOBS = "Delete all jobs from the scheduler.";
    public static final String OPERATION_ID_DELETE_ALL_JOBS = "deleteAllJobs";

    private final JobQuartzService jobQuartzService;

    public QuartzController(JobQuartzService jobQuartzService) {
        this.jobQuartzService = jobQuartzService;
    }

    @GetMapping(API_KEYS)
    @Operation(
            summary = SUMMARY_GET_JOB_KEYS,
            description = DESCRIPTION_GET_JOB_KEYS,
            operationId = OPERATION_ID_GET_JOB_KEYS,
            tags = { TAG_SCHEDULER }
    )
    public ResponseEntity<List<String>> getJobKeys() {
        return ResponseEntity.ok(jobQuartzService.getAllJobKeys());
    }

    @GetMapping(API_DETAILS)
    @Operation(
            summary = SUMMARY_GET_JOB_DETAIL,
            description = DESCRIPTION_GET_JOB_DETAIL,
            operationId = OPERATION_ID_GET_JOB_DETAIL,
            tags = { TAG_SCHEDULER }
    )
    public ResponseEntity<List<Map<String, Object>>> getJobDetails() {
        return ResponseEntity.ok(jobQuartzService.getAllJobDetails());
    }

    @GetMapping(API_ALL)
    @Operation(
            summary = SUMMARY_GET_ALL_JOBS,
            description = DESCRIPTION_GET_ALL_JOBS,
            operationId = OPERATION_ID_GET_ALL_JOBS,
            tags = { TAG_SCHEDULER }
    )
    public ResponseEntity<List<Map<String, Object>>> getAllJobs() {
        return ResponseEntity.ok(jobQuartzService.getAllJobs());
    }

    @PostMapping(API_TRIGGER)
    @Operation(
            summary = SUMMARY_TRIGGER_JOB,
            description = DESCRIPTION_TRIGGER_JOB,
            operationId = OPERATION_ID_TRIGGER_JOB,
            tags = { TAG_SCHEDULER }
    )
    public ResponseEntity<Boolean> triggerJob(@RequestParam String jobName) {
        return ResponseEntity.ok(jobQuartzService.triggerJob(jobName));
    }

    @PostMapping(API_DELETE)
    @Operation(
            summary = SUMMARY_DELETE_JOB,
            description = DESCRIPTION_DELETE_JOB,
            operationId = OPERATION_ID_DELETE_JOB,
            tags = { TAG_SCHEDULER }
    )
    public ResponseEntity<Boolean> deleteJob(@RequestParam String jobName) {
        return ResponseEntity.ok(jobQuartzService.deleteJob(jobName));
    }

    @PostMapping(API_DELETE_ALL)
    @Operation(
            summary = SUMMARY_DELETE_ALL_JOBS,
            description = DESCRIPTION_DELETE_ALL_JOBS,
            operationId = OPERATION_ID_DELETE_ALL_JOBS,
            tags = { TAG_SCHEDULER }
    )
    public ResponseEntity<Boolean> deleteAllJobs() {
        return ResponseEntity.ok(jobQuartzService.deleteAllJobs());
    }
}
