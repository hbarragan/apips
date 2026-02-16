package com.adasoft.pharmasuite.apips.api.dashboard.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "EquipmentsRooms data")
public class EquipmentsRooms {
    private String availableWDRooms;
    private String productionRooms;
    private String packagingLines;
}
