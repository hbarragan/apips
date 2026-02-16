package com.adasoft.pharmasuite.apips.api.common.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error information returned by the API")
public class ErrorApi {
    @Schema(description = "Internal error code", example = "ERR-001")
    private String code;
    @Schema(description = "Detailed description of the error", example = "Batch not found in the system")
    private String description;
    @Schema(description = "Human readable error message", example = "Invalid batch name")
    private String message;

}