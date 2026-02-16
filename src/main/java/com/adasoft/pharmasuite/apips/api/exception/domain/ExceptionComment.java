package com.adasoft.pharmasuite.apips.api.exception.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionComment implements Serializable {
    @Schema(nullable = true)
    private OffsetDateTime creationTime;
    private String description;
    private String capaId;
    private String externalIdentifier;

}
