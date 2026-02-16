package com.adasoft.pharmasuite.apips.api.batch.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.BaseDomain;
import com.adasoft.pharmasuite.apips.api.common.domain.Material;
import com.adasoft.pharmasuite.apips.api.common.domain.MeasureValue;
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
public class Sublot extends BaseDomain implements Serializable {
    private String name;
    private Long key;
    private MeasureValue quantity;
    private MeasureValue quantityConsumed;
    private Material material;
    private String batchIdentifier;
    private String productionOrderStep;
    private String storageLocation;
    private String storageArea;
    private String warehouse;
    private String tare;
    @Schema(nullable = true)
    private OffsetDateTime productionDate;

}
