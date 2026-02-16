package com.adasoft.pharmasuite.apips.api.exception.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.BaseDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionPS extends BaseDomain implements Serializable {
    private long key;
    private String description;
    @Schema(nullable = true)
    private OffsetDateTime creationDate;
    private String risk;
    private String category;
    private String result;
    private String classification;
    private String status;
    private String capaId;
    private String reference;

    private List<ExceptionComment> comments;
}