package com.adasoft.pharmasuite.apips.api.batch.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.BaseDomain;
import com.adasoft.pharmasuite.apips.api.common.domain.Material;
import com.adasoft.pharmasuite.apips.api.common.domain.MeasureValue;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@Schema(description = "Batch data")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Batch extends BaseDomain implements Serializable  {
    private long key;
    private String name;
    private String status;
    private Material material;
    private MeasureValue quantity;
    private MeasureValue potency;
    private MeasureValue totalConsumed;
    @Schema(nullable = true)
    private OffsetDateTime creationTime;
    @Schema(nullable = true)
    private OffsetDateTime expiryDate;
    @Schema(nullable = true)
    private OffsetDateTime retestDate;
    @Schema(nullable = true)
    private OffsetDateTime productionDate;
    @Schema(nullable = true)
    private OffsetDateTime nextInspectionDate;
    private List<TransactionHistory> transactionHistory;
}