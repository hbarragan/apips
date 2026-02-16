package com.adasoft.pharmasuite.apips.api.dashboard.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.BaseDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard data")
public class Dashboard extends BaseDomain implements Serializable {
    private Production production;
    private Exceptions exceptions;
    private EquipmentsRooms equipmentsRooms;
    private CleaningsCalibration cleaningsCalibration;

}
