package com.adasoft.pharmasuite.apips.health.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataEntry {
    String data;

    public DataEntry() {

    }

    public DataEntry(String data) {
    this.data=data;
    }
}

