package com.adasoft.pharmasuite.apips.api.workflow.domain;


import com.adasoft.pharmasuite.apips.api.common.domain.BaseDomain;
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
@Schema(description = "WorkFlow data")
public class  Workflow extends BaseDomain implements Serializable {
    private long key;
    private String name;
    private String description;
    private String status;
    private boolean productionRelevant;
    private String masterWorkflowName;
    private String masterWorkflowDescription;
    @Schema(nullable = true)
    private OffsetDateTime creationDate;
    @Schema(nullable = true)
    private OffsetDateTime actualStart;
    @Schema(nullable = true)
    private OffsetDateTime actualFinish;
    private boolean appendable;
    private String ordersAssociated;
    private List<String> upAssociated;
    @Schema(nullable = true)
    private String accessPrivilege;
    private MeasureValue quantity;
}
