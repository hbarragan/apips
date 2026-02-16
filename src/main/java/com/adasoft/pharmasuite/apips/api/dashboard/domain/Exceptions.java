package com.adasoft.pharmasuite.apips.api.dashboard.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exceptions data")
public class Exceptions {
    private Long ordersRunning;
    private Long ordersFinished;
    private String averageTimeOpenException;
    private Integer openExceptionRiskNone;
    private Integer openExceptionRiskLow;
    private Integer openExceptionRiskMedium;
    private Integer openExceptionRiskHigh;
}
