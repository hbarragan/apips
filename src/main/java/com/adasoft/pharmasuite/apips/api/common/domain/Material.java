package com.adasoft.pharmasuite.apips.api.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Material information")
public class Material {

    @Schema(description = "Unique identifier of the material", example = "1001")
    private long id;
    @Schema(description = "Name of the material", example = "Lactose Monohydrate")
    private String name;
    @Schema(description = "Category of the material", example = "Excipient")
    private String category;
    @Schema(description = "Detailed description of the material")
    private String description;
    @Schema(description = "Default unit of measure", example = "kg")
    private String unitOfMeasure;

}
