package com.adasoft.pharmasuite.apips.api.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Base domain object containing common fields like errors and server time.")
public abstract class BaseDomain {
    @Schema(description = "List of errors associated with the request or processing")
    @JsonIgnore
    private List<ErrorApi> errors;
    @Schema(description = "Server time when the response was generated", nullable = true)
    @JsonIgnore
    private OffsetDateTime timeServer;

    public void addError(ErrorApi error) {
        if(errors == null || errors.isEmpty()){
            errors = new ArrayList<>();
        }
        errors.add(error);
    }
}