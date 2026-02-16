package com.adasoft.pharmasuite.apips.api.common.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Represents a measured value with its unit")
public class MeasureValue implements Serializable {

    @Schema(description = "Unit of measure", example = "kg")
    private String unitOfMeasure;
    @Schema(description = "Numeric value", example = "150.50")
    private BigDecimal value;
    @Schema(description = "Precision/scale of the value", example = "2")
    private int scale;

}
