package com.adasoft.pharmasuite.apips.health.type;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum HealthyEnum {
    HEALTHY("Healthy"),
    UNHEALTHY("Unhealthy"),
    DEGRADED("Degraded");

    private final String status;

    HealthyEnum(String status) {
        this.status = status;
    }

    @JsonValue
    public String getStatus() {
        return status;
    }
}
