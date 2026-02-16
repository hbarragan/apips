package com.adasoft.pharmasuite.apips.api.order.domain;

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
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Process order data")
public class ProcessOrder extends BaseDomain implements Serializable {
    private long key;
    private String name;
    private MeasureValue quantity;
    private String quantityFinal;
    private MeasureValue consumedQuantity;
    private String targetBatch;
    private Material material;
    @Schema(nullable = true)
    private OffsetDateTime creationDate;
    @Schema(nullable = true)
    private OffsetDateTime plannedStart;
    @Schema(nullable = true)
    private OffsetDateTime plannedFinish;
    @Schema(nullable = true)
    private OffsetDateTime actualStart;
    @Schema(nullable = true)
    private OffsetDateTime actualFinish;
    @Schema(nullable = true)
    private OffsetDateTime erpFinishDate;
    @Schema(nullable = true)
    private OffsetDateTime erpStartDate;
    private String status;
    private List<String> wfAssociated;
    private String workCenter;
    private String location;
    private String recipeName;
    private String recipeDescription;
    private String batchName;
    @Schema(nullable = true, example = "JC_Visualizacion")
    private String accessPrivilege;
}
