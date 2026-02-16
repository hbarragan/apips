package com.adasoft.pharmasuite.apips.api.dashboard.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CleaningsCalibration data")
public class CleaningsCalibration {
    private String roomsEquipmentToBeCleaned;
    private String equipmentToExpiredClean;
    private String roomToExpiredClean;

    private String equipmentNotCalibrated;
    private String equipmentToBeCalibrated;
    private String equipmentToBeTested;
}
