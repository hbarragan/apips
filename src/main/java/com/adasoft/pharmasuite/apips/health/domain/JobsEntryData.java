package com.adasoft.pharmasuite.apips.health.domain;

import lombok.Getter;

@Getter
public class JobsEntryData extends DataEntry {
    Object jobs;

    public JobsEntryData(Object jobs) {
        this.jobs = jobs;
    }

}