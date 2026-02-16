package com.adasoft.pharmasuite.apips.api.common.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public record PageResponseOdata<T>(
        @JsonProperty("@odata.context")
        String context,
        @JsonProperty("@odata.count")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long count,
        List<T> value
){}
