package com.adasoft.pharmasuite.apips.api.batch.domain;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
@Schema(description = "Filter parameters for Sublot queries.")
public class SublotFilter {

    @Schema(
            description = "Filter sublot by batch name",
            example = "Batch 1"
    )
    @Parameter()
    private String batchName;

    @Schema(
            description = "Filter sublot by batch key",
            example = "1234"
    )
    @Parameter()
    private Long batchKey;

}
