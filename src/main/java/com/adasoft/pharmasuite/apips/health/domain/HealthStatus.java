package com.adasoft.pharmasuite.apips.health.domain;

import com.adasoft.pharmasuite.apips.health.type.HealthyEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@Data
public class HealthStatus {
    private HealthyEnum status;
    private String totalDuration;
    private Map<String, Entry> entries;

    @JsonIgnore
    public boolean isHealthy() {
        return status != null && status.getStatus().equals(HealthyEnum.HEALTHY.getStatus());
    }

    public HealthStatus() {}
}
