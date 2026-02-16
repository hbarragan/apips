package com.adasoft.pharmasuite.apips.api.recipe.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.MeasureValue;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BomItems {
        private String materialIdentifier;
        private String materialDescription;
        private MeasureValue quantity;
        private String position;
}
