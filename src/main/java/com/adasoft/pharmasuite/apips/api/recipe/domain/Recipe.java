package com.adasoft.pharmasuite.apips.api.recipe.domain;

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
@Schema(description = "Basic recipe data")
public class Recipe extends BaseDomain implements Serializable  {
    private long key;
    private String name;
    private String status;
    private String revision;
    @Schema(nullable = true)
    private OffsetDateTime creationDate;
    @Schema(nullable = true)
    private OffsetDateTime effectivityStartTime;
    @Schema(nullable = true)
    private OffsetDateTime effectivityEndTime;
    private Material material;
    private MeasureValue quantity;
    private String procedure;
    private String accessPrivilege;
    private List<BomItems> bomItemsList;
}
