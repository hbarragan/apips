package com.adasoft.pharmasuite.apips.health.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.adasoft.pharmasuite.apips.health.type.HealthyEnum;

import java.util.List;

@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Entry {

    private HealthyEnum status;
    private DataEntry data;
    private String duration;
    private List<String> tags;
    private String description;

    public Entry(){}

    public Entry(HealthyEnum status, String duration, List<String> tags, String description) {
        this.status = status;
        this.duration = duration;
        this.tags = tags;
        this.description = description;
    }

    public Entry(HealthyEnum status, String duration, String description) {
        this.status = status;
        this.duration = duration;
        this.tags = null;
        this.description = description;
    }

}

