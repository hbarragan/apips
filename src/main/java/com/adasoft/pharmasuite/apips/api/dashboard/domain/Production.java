package com.adasoft.pharmasuite.apips.api.dashboard.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Production data")
public class Production {
    private String orderExecution;
    private String WDOrders;
    private String productionOrders;
    private String packagingOrders;

}
