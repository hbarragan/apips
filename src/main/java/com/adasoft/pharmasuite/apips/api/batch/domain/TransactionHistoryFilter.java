package com.adasoft.pharmasuite.apips.api.batch.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterCache;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Schema(description = "Filter parameters for transaction history queries.")
public class TransactionHistoryFilter {

    @Schema(
            description = "Filter transaction history by batch name",
            example = "Batch 1"
    )
    @Parameter()
    private String batchName;

    @Schema(
            description = "Filter transaction history by batch key",
            example = "1234"
    )
    @Parameter()
    private Long batchKey;

    @Parameter(
            name = "orderAssociateType",
            in = ParameterIn.QUERY,
            description = "Filter by state. [OUTPUT, INPUT ...]",
            explode = Explode.TRUE,
            schema = @Schema(type = "array", allowableValues = {""})
    )
    private List<String> orderAssociateType;

    @Parameter(
            name = "orderAssociateSubtype",
            in = ParameterIn.QUERY,
            description = "Filter by state. [PRODUCTION_OF_OUTPUT_MATERIAL, BATCH_GENERATION ...]",
            explode = Explode.TRUE,
            schema = @Schema(type = "array", allowableValues = {""})
    )
    private List<String> orderAssociateSubtype;

}
